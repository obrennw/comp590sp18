package apps;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import codec.ArithmeticDecoder;
import codec.ArithmeticEncoder;
import codec.SymbolDecoder;
import codec.SymbolEncoder;
import io.BitSink;
import io.BitSource;
import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;
import io.OutputStreamBitSink;
import models.Symbol;
import models.SymbolModel;
import models.Unsigned12BitModel;
import models.Unsigned12BitModel.Unsigned12BitSymbol;

//enum VideoTraversalOrder {
//	WIDTH_HEIGHT_FRAME,
//	WIDTH_FRAME_HEIGHT,
//	HEIGHT_WIDTH_FRAME,
//	HEIGHT_FRAME_WIDTH,
//	FRAME_WIDTH_HEIGHT,
//	FRAME_HEIGHT_WIDTH
//}

public class LZWAppFixed {
	
	public static void main(String[] args) throws IOException, InsufficientBitsLeftException {
		long start = System.nanoTime();
		long e9 = 1000000000;
		
		int width = 800;
		int height = 450;
		int num_frames = 150;
		int size = width*height*num_frames;
		//int tbit = 4096;
		int tbit = 65536;
		//int tbit = 1048576;
		//int tbit = 2097152;
		//int tbit = 4194304;
		//int tbit = 16777216;
		//int tbit = 1073741824;
		
		//int lbit = 12;
		int lbit = 16;
		//int lbit = 20;
		//int lbit = 21;
		//int lbit = 22;
		//int lbit = 24;
		//int lbit = 30;

		
		//String base = args[0];
		String base = "bunny";
		//String vto = args[1];
		String fixed = "20fixed-";
		String vto = "fwh";
		//String process = args[2];
		String process = "decode";
		VideoTraversalOrder order = VideoTraversalOrder.WIDTH_HEIGHT_FRAME;
		if(process.equals("encode")){
			switch(vto){
				case "whf":
					order = VideoTraversalOrder.WIDTH_HEIGHT_FRAME;
					break;
				case "wfh":
					order = VideoTraversalOrder.WIDTH_FRAME_HEIGHT;
					break;
				case "hwf":
					order = VideoTraversalOrder.HEIGHT_WIDTH_FRAME;
					break;
				case "hfw":
					order = VideoTraversalOrder.HEIGHT_FRAME_WIDTH;
					break;
				case "fwh":
					order = VideoTraversalOrder.FRAME_WIDTH_HEIGHT;
					break;
				case "fhw":
					order = VideoTraversalOrder.FRAME_HEIGHT_WIDTH;
					break;
				default:
					System.out.println("Invalid video traversal order! Exiting...");
					System.exit(1);
			}
			
			PrintStream output = new PrintStream(new BufferedOutputStream(
					new FileOutputStream(
							"/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+base+"-encodeOutput-"+fixed+vto+".txt")
					), true);
			System.setOut(output);
			String filename="/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+ base + ".450p.yuv";
			File file = new File(filename);
	
			InputStream training_values = new FileInputStream(file);
			byte [][][] video = new byte [width][height][num_frames];
			for(int i=0; i<num_frames; i++){
				for(int j=0; j<height; j++){
					for(int k=0; k<width; k++){
						video[k][j][i] = (byte)training_values.read();
					}
				}
			}
	
			int [] compressed = new int[0]; //LZWApp.lzwCompress(training_values,tbit);
			//LZWApp.lzwCompressVideo(video, tbit,num_frames,height,width);
	
			switch(order){
				case WIDTH_HEIGHT_FRAME:
					compressed = LZWAppFixed.lzwCompressVideo(video, tbit,num_frames,height,width);
					break;
				case WIDTH_FRAME_HEIGHT:
					compressed = LZWAppFixed.lzwCompressVideo(video, tbit,height,num_frames,width);
					break;
				case HEIGHT_WIDTH_FRAME:
					compressed = LZWAppFixed.lzwCompressVideo(video, tbit,num_frames,width,height);
					break;
				case HEIGHT_FRAME_WIDTH:
					compressed = LZWAppFixed.lzwCompressVideo(video, tbit,width,num_frames,height);
					break;
				case FRAME_HEIGHT_WIDTH:
					compressed = LZWAppFixed.lzwCompressVideo(video, tbit,width,height,num_frames);
					break;
				case FRAME_WIDTH_HEIGHT:
					compressed = LZWAppFixed.lzwCompressVideo(video, tbit,height,width,num_frames);
					break;	
			}
								  
			long compression = System.nanoTime()-start;
			System.out.println("Compression Time: " +((compression/e9)/60)+"m "+((compression/e9)%60)+"s");
			int compressed_length = compressed.length;
			System.out.println("Array size: "+compressed_length);
			video = new byte[0][0][0];
			training_values.close();
	
			File out_file = new File("/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+ base + "-compressed-"+fixed+vto+".dat");
			//File out_file = new File("/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+ base + "-compressed16.dat");
			OutputStream out_stream = new FileOutputStream(out_file);
			BitSink bit_sink = new OutputStreamBitSink(out_stream);
			
			long training = System.nanoTime()-start;
			System.out.println("Training Time: " +((training/e9)/60)+"m "+((training/e9)%60)+"s");
	
			LZWAppFixed.encodeArray(compressed, bit_sink, lbit);
			System.out.println("Array Encoded");
			long encoding = System.nanoTime()-start;
			System.out.println("Encoding Time: " +((encoding/e9)/60)+"m "+((encoding/e9)%60)+"s");
			PrintWriter wr = new PrintWriter("/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+ base + ""+fixed+vto+"-compressed-size.txt");
			wr.println(compressed_length);
			wr.close();
			long runtime = System.nanoTime()-start;
			System.out.println("Output Time: " +((runtime/e9)/60)+"m "+((runtime/e9)%60)+"s");
			
		} else if(process.equals("decode")){
			PrintStream output = new PrintStream(new BufferedOutputStream(
					new FileOutputStream(
							"/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+base+"-decodeOutput-"+fixed+vto+".txt")
					), true);
			System.setOut(output);
			File c = new File("/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+ base + ""+fixed+vto+"-compressed-size.txt");
			Scanner cs = new Scanner(c);
			int compressed_length = cs.nextInt();
			File out_file = new File("/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+ base + "-compressed-"+fixed+vto+".dat");
			BitSource bit_source = new InputStreamBitSource(new FileInputStream(out_file));
			OutputStream decoded_file = new FileOutputStream(new File("/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+ base + "-decoded-"+fixed+vto+".dat"));
			int [] decodedList = LZWAppFixed.decodeArray(bit_source, compressed_length, lbit);
			//compressed;
			System.out.println("Array Decoded");
			//System.out.println("Array Equal: "+Arrays.equals(compressed, decodedList));
			long decoding = System.nanoTime()-start;
			System.out.println("Decoding Time: " +((decoding/e9)/60)+"m "+((decoding/e9)%60)+"s");
			//LZWApp.decompressAndOutputLZW(decodedList, decoded_file,tbit);
			byte [] videoList = LZWAppFixed.decompressLZWVideo(decodedList, tbit,size);
			long decompression = System.nanoTime()-start;
			System.out.println("Decompression Time: " +((decompression/e9)/60)+"m "+((decompression/e9)%60)+"s");
			LZWAppFixed.outputDecompressedVideo(decoded_file, videoList, order, width, height, num_frames);
			decoded_file.close();
			//long decompression = System.nanoTime()-encoding;
			long runtime = System.nanoTime()-start;
			System.out.println("Output Time: " +((runtime/e9)/60)+"m "+((runtime/e9)%60)+"s");
			
		}
	
	}

	private static int[] lzwCompress(InputStream src, int limit) throws IOException{
		HashMap<ArrayList<Byte>,Integer> dict = new HashMap<ArrayList<Byte>,Integer>();
		for(int i=0; i<256; i++){
			ArrayList<Byte> list = new ArrayList<Byte>();
			list.add(Byte.valueOf(((byte)i)));
			dict.put(list, i);
		}
		ArrayList<Byte> w = new ArrayList<Byte>();
		ArrayList<Integer> res = new ArrayList<Integer>();
		int read =  src.read();
		while(read != -1){
			Byte c = (byte)read;
			ArrayList<Byte> wc = new ArrayList<Byte>();
			wc.addAll(w);
			wc.add(c);
			if(dict.containsKey(wc)){
				w = wc;
			} else{
				res.add(dict.get(w));
				if(dict.size()<limit){
					dict.put(wc, dict.size());
				}
				ArrayList<Byte> cl = new ArrayList<Byte>();
				cl.add(c);
				w = cl;
			}
			read = src.read();
		}
		ArrayList<Byte> empty = new ArrayList<Byte>();
		if(!(w.equals(empty))){
			res.add(dict.get(w));
		}
		System.out.println("Compress hashmap size: "+ dict.size());
		return res.stream().mapToInt(Integer::intValue).toArray();
	}
	
	private static int[] lzwCompressVideo(byte[][][] video, int limit, int i_limit, int j_limit, int k_limit) throws IOException{
		HashMap<ArrayList<Byte>,Integer> dict = new HashMap<ArrayList<Byte>,Integer>();
		for(int i=0; i<256; i++){
			ArrayList<Byte> list = new ArrayList<Byte>();
			list.add(Byte.valueOf(((byte)i)));
			dict.put(list, i);
		}
		ArrayList<Byte> w = new ArrayList<Byte>();
		ArrayList<Integer> res = new ArrayList<Integer>();
		for(int i= 0; i< i_limit; i++){
			for(int j= 0; j< j_limit; j++){
				for(int k= 0; k< k_limit; k++){
					Byte c = getByteFromVideo(video,i_limit,j_limit,k_limit,i,j,k);
					ArrayList<Byte> wc = new ArrayList<Byte>();
					wc.addAll(w);
					wc.add(c);
					if(dict.containsKey(wc)){
						w = wc;
					} else{
						res.add(dict.get(w));
						if(dict.size()<limit){
							dict.put(wc, dict.size());
						}
						ArrayList<Byte> cl = new ArrayList<Byte>();
						cl.add(c);
						w = cl;
					}
				}
			}
		}
		ArrayList<Byte> empty = new ArrayList<Byte>();
		if(!(w.equals(empty))){
			res.add(dict.get(w));
		}
		System.out.println("Compress hashmap size: "+ dict.size());
		dict = null; 
		return res.stream().mapToInt(Integer::intValue).toArray();
	}
	
	private static byte getByteFromVideo(byte[][][] video, int i_limit, int j_limit, int k_limit, int i, int j, int k){
		//byte [][][] video = new byte [width][height][num_frames];
		if(video[0][0].length == i_limit && video[0].length == j_limit && video.length == k_limit){
			return video[k][j][i];
		}
		else if(video[0][0].length == i_limit && video.length == j_limit && video[0].length == k_limit){
			return video[j][k][i];
		}
		else if(video.length == i_limit && video[0][0].length == j_limit && video[0].length == k_limit){
			return video[i][k][j];
		}
		else if(video.length == i_limit && video[0].length == j_limit && video[0][0].length == k_limit){
			return video[i][j][k];
		}
		else if(video[0].length == i_limit && video.length == j_limit && video[0][0].length == k_limit){
			return video[j][i][k];
		}
		else if(video[0].length == i_limit && video[0][0].length == j_limit && video.length == k_limit){
			return video[k][i][j];
		}
		return (byte)0;
	}
	
	public static void encodeArray(int [] arr, BitSink bit_sink, int lbit) 
			throws IOException {
		for(int i=0; i<arr.length; i++){
		    bit_sink.write(arr[i], lbit);
		}
		arr = null;
	}
	
	private static int [] decodeArray(BitSource bit_source, int size, int lbit) 
			throws InsufficientBitsLeftException, IOException {
		ArrayList<Integer> res = new ArrayList<Integer>();
		for(int i=0; i<size; i++){
			res.add(bit_source.next(lbit));
		}
		return res.stream().mapToInt(Integer::intValue).toArray();
	}

	private static void decompressAndOutputLZW(int[] list, OutputStream out,int limit) throws IOException {
		HashMap<Integer,ArrayList<Byte>> dict = new HashMap<Integer,ArrayList<Byte>>();
		for(int i=0; i<256; i++){
			ArrayList<Byte> blist = new ArrayList<Byte>();
			blist.add(Byte.valueOf((byte)i));
			dict.put(i,blist);
		}
		ArrayList<Byte> w = new ArrayList<Byte>();
		w.addAll(dict.get((list[0])));
		for(Byte b: w){
			out.write(b.byteValue());
		}
		for(int i=1; i< list.length; i++){
			ArrayList<Byte> dictStr = new ArrayList<Byte>();
			if (list[i] >= dict.size()){
				ArrayList<Byte> res = new ArrayList<Byte>();
				res.addAll(w);
				res.add(w.get(0));
				dictStr = res;
			} else {
				dictStr = dict.get(list[i]);
			}
			for(Byte b: dictStr){
				out.write(b.byteValue());
			}
			if(dict.size()<limit){
				ArrayList<Byte> dictl = new ArrayList<Byte>();
				dictl.addAll(w);
				dictl.add(dictStr.get(0));
				dict.put(dict.size(),dictl);
			} 
			w = dictStr;
		}
		System.out.println("Decompressed hashmap/list size: "+ dict.size());
		System.out.println("Finished Decompression");
	}
	
	private static byte[] decompressLZWVideo(int[] list,int limit, int size) throws IOException {
		byte [] video = new byte[size];
		int z = 0;
		HashMap<Integer,ArrayList<Byte>> dict = new HashMap<Integer,ArrayList<Byte>>();
		for(int i=0; i<256; i++){
			ArrayList<Byte> blist = new ArrayList<Byte>();
			blist.add(Byte.valueOf((byte)i));
			dict.put(i,blist);
		}
		ArrayList<Byte> w = new ArrayList<Byte>();
		w.addAll(dict.get((list[0])));
		for(Byte b: w){
			video[z++] = (byte)b;
			//out.write(b.byteValue());
		}
		for(int i=1; i< list.length; i++){
			ArrayList<Byte> dictStr = new ArrayList<Byte>();
			if (list[i] >= dict.size()){
				ArrayList<Byte> res = new ArrayList<Byte>();
				res.addAll(w);
				res.add(w.get(0));
				dictStr = res;
			} else {
				dictStr = dict.get(list[i]);
			}
			for(Byte b: dictStr){
				video[z++] = (byte)b;
				//out.write(b.byteValue());
			}
			if(dict.size()<limit){
				ArrayList<Byte> dictl = new ArrayList<Byte>();
				dictl.addAll(w);
				dictl.add(dictStr.get(0));
				dict.put(dict.size(),dictl);
			} 
			w = dictStr;
		}
		System.out.println("Decompressed hashmap/list size: "+ dict.size());
		System.out.println("Returning Video");
		return video;
	}
	
	private static void outputDecompressedVideo(OutputStream out, byte[] videoList,VideoTraversalOrder order, int width,int height, int num_frames) throws IOException{
		byte[][][] video = new byte[width][height][num_frames];
		int count = 0;
		switch(order){
			case WIDTH_HEIGHT_FRAME:
				for(int i=0; i<num_frames; i++){
					for(int j=0; j<height; j++){
						for(int k=0; k<width; k++){
							video[k][j][i] = videoList[count++];
						}
					}
				}
				break;
			case WIDTH_FRAME_HEIGHT:
				for(int i=0; i<height; i++){
					for(int j=0; j<num_frames; j++){
						for(int k=0; k<width; k++){
							video[k][i][j] = videoList[count++];
						}
					}
				}
				break;
			case HEIGHT_WIDTH_FRAME:
				for(int i=0; i<num_frames; i++){
					for(int j=0; j<width; j++){
						for(int k=0; k<height; k++){
							video[j][k][i] = videoList[count++];
						}
					}
				}
				break;
			case HEIGHT_FRAME_WIDTH:
				for(int i=0; i<width; i++){
					for(int j=0; j<num_frames; j++){
						for(int k=0; k<height; k++){
							video[i][k][j] = videoList[count++];
						}
					}
				}
				break;
			case FRAME_HEIGHT_WIDTH:
				for(int i=0; i<width; i++){
					for(int j=0; j<height; j++){
						for(int k=0; k<num_frames; k++){
							video[i][j][k] = videoList[count++];
						}
					}
				}
				break;
			case FRAME_WIDTH_HEIGHT:
				for(int i=0; i<height; i++){
					for(int j=0; j<width; j++){
						for(int k=0; k<num_frames; k++){
							video[j][i][k] = videoList[count++];
						}
					}
				}
				break;	
		}
		System.out.println("Re-constructed Video");
		for(int i=0; i<num_frames; i++){
			for(int j=0; j<height; j++){
				for(int k=0; k<width; k++){
					out.write(video[k][j][i]);
				}
			}
		}
		System.out.println("Finished Decompression");
	}
	
}


