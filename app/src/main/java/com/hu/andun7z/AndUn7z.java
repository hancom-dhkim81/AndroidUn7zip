package com.hu.andun7z;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.util.Log;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.jetbrains.annotations.NotNull;

public class AndUn7z {

	public boolean extract7zJava(String filePath, String outPath)
	{
		File outDir = new File(outPath);
		if(!outDir.exists() || !outDir.isDirectory())
		{
			outDir.mkdirs();
		}

		createNoMediaFile(outPath);

		return (un7zipJava(filePath, outPath) == 1);
	}
	
	public boolean extract7z(String filePath, String outPath)
	{
		File outDir = new File(outPath);
		if(!outDir.exists() || !outDir.isDirectory())
		{
			outDir.mkdirs();
		}

		createNoMediaFile(outPath);

		int count = getFileCount(filePath);
		Log.e("entryCnt", "EntryCount :  " + count);

		return (un7zip(filePath, outPath) == 1);
	}
	
	/**
	 * Extract from assets
	 * @param context
	 * @param assetPath
	 * @param outPath
	 * @return
	 * @throws Exception
	 */
	public boolean extractAssets(Context context, String assetPath, String outPath)
	{
		File outDir = new File(outPath);
		if(!outDir.exists() || !outDir.isDirectory())
		{
			outDir.mkdirs();
		}

		createNoMediaFile(outPath);

		
		String tempPath = outPath + File.separator + ".temp";
		try {
			long start = System.currentTimeMillis();

			copyFromAssets(context, assetPath, tempPath);

			long end = System.currentTimeMillis();
			long spent = end - start;
			Log.e("TIME", "Time : " + spent / 1000 + "secs");

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		boolean ret = (un7zip(tempPath, outPath) == 1);
		new File(tempPath).delete();
		
		return ret;
	}

	private void createNoMediaFile(String outPath) {
		String nomediaPath = outPath + File.separator + ".nomedia";
		File nomediaFile = new File(nomediaPath);
		if (!nomediaFile.exists()) {
			try {
				nomediaFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Copy asset to temp
	 * @param context
	 * @param assetPath
	 * @param tempPath
	 * @throws Exception
	 */
	private void copyFromAssets(Context context, String assetPath, String tempPath)
			throws Exception
	{
		InputStream inputStream = context.getAssets().open(assetPath);
		BufferedInputStream bis = new BufferedInputStream(inputStream);

		FileOutputStream fileOutputStream = new FileOutputStream(tempPath);
		BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);

		int length = -1;
		byte[] buffer = new byte[1024*1024*15];
		while ((length = bis.read(buffer)) != -1) {
			bos.write(buffer, 0, length);
		}

		bos.flush();
		bos.close();
		fileOutputStream.close();
		bis.close();
		inputStream.close();
	}

	//JNI interface
	private native int un7zip(String filePath, String outPath);
	private native int getFileCount(String filePath);

	private void progressCallback(int count, int total) {
		Log.d("JNI", "( " +count + " / " + total + " )");
	}

	private void testCallback(String param) {
		Log.d("JNI", param);
	}

	private void updateTimer() {
		Log.d("JNI", "Call updateTimer");
	}

	private static int BUF_SIZE = 4096; // 4 k
	private static int BUF_100k_SIZE = 1024 * 100; // 100 k
	private static int BUF_300k_SIZE = 1024 * 300; // 300 k
	private static int BUF_500k_SIZE = 1024 * 500; // 300 k
	private static int BUF_1M_SIZE = 1024 * 1024; // 1M
	private static int BUF_5M_SIZE = 1024 * 1024 * 5; // 5M
	private static int BUF_10M_SIZE = 1024 * 1024 * 10; // 10M

	static byte[] BUFFER = new byte[BUF_SIZE];
	static byte[] BUFFER_100K = new byte[BUF_100k_SIZE];
	static byte[] BUFFER_300K = new byte[BUF_300k_SIZE];
	static byte[] BUFFER_500K = new byte[BUF_500k_SIZE];
	static byte[] BUFFER_1M = new byte[BUF_1M_SIZE];
	static byte[] BUFFER_5M = new byte[BUF_5M_SIZE];

	private int un7zipJava2(String filePath, String outPath) {

		java.io.File inFile = new java.io.File(filePath);
		java.io.File outFile = new java.io.File(outPath);

		try {

			SevenZFile sevenZFile = new SevenZFile(inFile);
			SevenZArchiveEntry entry = null;

			while ((entry = sevenZFile.getNextEntry()) != null) {

				String entryName = entry.getName();

				if (entry.isDirectory()) {
					new File(outPath, entryName).mkdirs();
					continue;
				} else {

					File out = new File(outPath, entryName);
					String parentPath = out.getParentFile().getAbsolutePath();

					File parentFile = new File(parentPath);
					if (!parentFile.exists()) {
						parentFile.mkdirs();
					}

					int entrySize = (int)entry.getSize();
					if (entrySize == 0) {
						out.createNewFile();
						continue;
					}

					FileOutputStream fos = new FileOutputStream(out);
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					byte[] buffer = BUFFER;

					int bufSize = BUF_SIZE;
					if (entrySize < BUF_SIZE) {
						bufSize = entrySize;
					} else if (entrySize > BUF_100k_SIZE && entrySize < BUF_300k_SIZE) {
						bufSize = BUF_100k_SIZE;
						buffer = BUFFER_100K;
					} else if (entrySize > BUF_300k_SIZE && entrySize < BUF_500k_SIZE) {
						bufSize = BUF_300k_SIZE;
						buffer = BUFFER_300K;
					} else if (entrySize > BUF_500k_SIZE && entrySize < BUF_1M_SIZE) {
						bufSize = BUF_500k_SIZE;
						buffer = BUFFER_500K;
					} else if (entrySize > BUF_1M_SIZE && entrySize < BUF_5M_SIZE) {
						bufSize = BUF_1M_SIZE;
						buffer = BUFFER_1M;
					} else if (entrySize > BUF_5M_SIZE && entrySize < BUF_10M_SIZE) {
						bufSize = BUF_5M_SIZE;
						buffer = BUFFER_5M;
					} else if (entrySize > BUF_10M_SIZE) {
						bufSize = BUF_10M_SIZE;
						buffer = new byte[BUF_10M_SIZE];
					}

					Log.e("dhkim", "" + out.getAbsolutePath() + "/" + bufSize + "/" + entrySize);

//					byte[] buffer = new byte[bufSize];

					int readData = 0;
					while ((readData = sevenZFile.read(buffer, 0, bufSize)) != -1) {
						bos.write(buffer, 0, readData);
					}

//					byte[] buffer = new byte[4096];
//
//					int readData = 0;
//					while ((readData = sevenZFile.read(buffer)) != -1) {
//						bos.write(buffer, 0, readData);
//					}

					bos.flush();
					bos.close();
					fos.close();
				}

			}

			sevenZFile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}


		return 1;
	}

	private int un7zipJava(String filePath, String outPath) {

		java.io.File inFile = new java.io.File(filePath);
		java.io.File outFile = new java.io.File(outPath);

		try {
			SevenZFile sevenZFile = new SevenZFile(inFile);

			SevenZArchiveInputStream ins = new SevenZArchiveInputStream(sevenZFile);

			ArchiveEntry entry = null;

			while ((entry = ins.getNextEntry()) != null) {

				String entryName = entry.getName();

				if (entry.isDirectory()) {
					new File(outPath, entryName).mkdirs();
				} else {

					File out = new File(outPath, entryName);
					String parentPath = out.getParentFile().getAbsolutePath();

					File parentFile = new File(parentPath);
					if (!parentFile.exists()) {
						parentFile.mkdirs();
					}

					int entrySize = (int)entry.getSize();
					if (entrySize == 0) {
						out.createNewFile();
						continue;
					}

					FileOutputStream fos = new FileOutputStream(out);
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					byte[] buffer = BUFFER;

					int bufSize = BUF_SIZE;
					if (entrySize < BUF_SIZE) {
						bufSize = entrySize;
					} else if (entrySize > BUF_100k_SIZE && entrySize < BUF_300k_SIZE) {
						bufSize = BUF_100k_SIZE;
						buffer = BUFFER_100K;
					} else if (entrySize > BUF_300k_SIZE && entrySize < BUF_500k_SIZE) {
						bufSize = BUF_300k_SIZE;
						buffer = BUFFER_300K;
					} else if (entrySize > BUF_500k_SIZE && entrySize < BUF_1M_SIZE) {
						bufSize = BUF_500k_SIZE;
						buffer = BUFFER_500K;
					} else if (entrySize > BUF_1M_SIZE && entrySize < BUF_5M_SIZE) {
						bufSize = BUF_1M_SIZE;
						buffer = BUFFER_1M;
					} else if (entrySize > BUF_5M_SIZE) {
						bufSize = BUF_5M_SIZE;
						buffer = BUFFER_5M;
					}

					Log.e("dhkim", "" + out.getAbsolutePath() + "/" + bufSize + "/" + entrySize);

					BufferedInputStream bis = new BufferedInputStream(ins);

					int readData = 0;
					while ((readData = bis.read(buffer, 0, bufSize)) != -1) {
						bos.write(buffer, 0, readData);
					}

					bos.flush();
					bos.close();
					fos.close();
				}

			}

			ins.close();
			sevenZFile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}


		return 1;
	}

//	private static int un7zipJava(String filePath, String outPath) {
//
//		java.io.File inFile = new java.io.File(filePath);
//		java.io.File outFile = new java.io.File(outPath);
//
//
//		try {
//			java.io.BufferedInputStream inStream  = new java.io.BufferedInputStream(new java.io.FileInputStream(inFile));
//			java.io.BufferedOutputStream outStream = new java.io.BufferedOutputStream(new java.io.FileOutputStream(outFile));
//
//			int propertiesSize = 5;
//			byte[] properties = new byte[propertiesSize];
//			if (inStream.read(properties, 0, propertiesSize) != propertiesSize)
//				throw new Exception("input .lzma file is too short");
//			SevenZip.Compression.LZMA.Decoder decoder = new SevenZip.Compression.LZMA.Decoder();
//			if (!decoder.SetDecoderProperties(properties))
//				throw new Exception("Incorrect stream properties");
//			long outSize = 0;
//			for (int i = 0; i < 8; i++)
//			{
//				int v = inStream.read();
//				if (v < 0)
//					throw new Exception("Can't read stream size");
//				outSize |= ((long)v) << (8 * i);
//			}
//			if (!decoder.Code(inStream, outStream, outSize))
//				throw new Exception("Error in data stream");
//
//			outStream.flush();
//			outStream.close();
//			inStream.close();
//
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return 1;
//	}

	public class SevenZArchiveInputStream extends ArchiveInputStream {
		private final SevenZFile mySevenZFile;

		public SevenZArchiveInputStream(@NotNull final File file) throws IOException {
			mySevenZFile = new SevenZFile(file);
		}
		public SevenZArchiveInputStream(@NotNull final SevenZFile file) {
			mySevenZFile = file;
		}

		@Override
		public ArchiveEntry getNextEntry() throws IOException {
			return mySevenZFile.getNextEntry();
		}

		@Override
		public int read(final byte[] b, final int off, final int len) throws IOException {
			return mySevenZFile.read(b, off, len);
		}

		@Override
		public void close() throws IOException {
			mySevenZFile.close();
			super.close(); // do nothing actually
		}
	}
	
	static {
		System.loadLibrary("un7z");
	}
}
