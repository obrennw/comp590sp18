package apps;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
import models.Unsigned14BitModel;
import models.Unsigned14BitModel.Unsigned14BitSymbol;
import models.Unsigned16BitModel;
import models.Unsigned16BitModel.Unsigned16BitSymbol;;

enum VideoTraversalOrder {
	WIDTH_HEIGHT_FRAME,
	WIDTH_FRAME_HEIGHT,
	HEIGHT_WIDTH_FRAME,
	HEIGHT_FRAME_WIDTH,
	FRAME_WIDTH_HEIGHT,
	FRAME_HEIGHT_WIDTH
}

public class LZWApp {
	
	public static void main(String[] args) throws IOException, InsufficientBitsLeftException {
		long start = System.nanoTime();
		long e9 = 1000000000;
		
		int width = 800;
		int height = 450;
		int num_frames = 150;
		
		int tbit = 4096;
		//int tbit = 16384;
		//int tbit = 65536;
		//int tbit = 524288;
		
		//String base = args[0];
		String base = "candle";
		//String vto = args[1];
		String vto = "hwf";
		VideoTraversalOrder order = VideoTraversalOrder.WIDTH_HEIGHT_FRAME;
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
						"/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/16"+base+"-output-"+vto+".txt")
				), true);
		System.setOut(output);
		String filename="/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+ base + ".450p.yuv";
		File file = new File(filename);
		Unsigned12BitModel model = new Unsigned12BitModel();
		//Unsigned14BitModel model = new Unsigned14BitModel();
		//Unsigned16BitModel model = new Unsigned16BitModel();
		//Unsigned24BitModel model = new Unsigned24BitModel();


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
				compressed = LZWApp.lzwCompressVideo(video, tbit,num_frames,height,width);
				break;
			case WIDTH_FRAME_HEIGHT:
				compressed = LZWApp.lzwCompressVideo(video, tbit,height,num_frames,width);
				break;
			case HEIGHT_WIDTH_FRAME:
				compressed = LZWApp.lzwCompressVideo(video, tbit,num_frames,width,height);
				break;
			case HEIGHT_FRAME_WIDTH:
				compressed = LZWApp.lzwCompressVideo(video, tbit,width,num_frames,height);
				break;
			case FRAME_HEIGHT_WIDTH:
				compressed = LZWApp.lzwCompressVideo(video, tbit,width,height,num_frames);
				break;
			case FRAME_WIDTH_HEIGHT:
				compressed = LZWApp.lzwCompressVideo(video, tbit,height,width,num_frames);
				break;	
		}
							  
		long compression = System.nanoTime()-start;
		System.out.println("Compression Time: " +((compression/e9)/60)+"m "+((compression/e9)%60)+"s");
		System.out.println("Array size: "+compressed.length);
		video = new byte[0][0][0];
		LZWApp.trainModelWithArray(model, compressed);
		training_values.close();

		//		HuffmanEncoder encoder = new HuffmanEncoder(model, model.getCountTotal());
		//		Map<Symbol, String> code_map = encoder.getCodeMap();

		SymbolEncoder encoder = new ArithmeticEncoder(model);

		Symbol[] symbols = new Unsigned12BitSymbol[tbit];
		//Symbol[] symbols = new Unsigned14BitSymbol[tbit];
		//Symbol[] symbols = new Unsigned16BitSymbol[tbit];
		//Symbol[] symbols = new Unsigned24BitSymbol[tbit];
		for (int v=0; v<tbit; v++) {
			SymbolModel s = model.getByIndex(v);
			Symbol sym = s.getSymbol();
			symbols[v] = sym;

			long prob = s.getProbability(model.getCountTotal());
			System.out.println("Symbol: " + sym + " probability: " + prob + "/" + model.getCountTotal());
		}	
		System.out.println("Sybmols generated");

		//InputStream message = new FileInputStream(file);

		File out_file = new File("/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/16"+ base + "-compressed-"+vto+".dat");
		//File out_file = new File("/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+ base + "-compressed16.dat");
		OutputStream out_stream = new FileOutputStream(out_file);
		BitSink bit_sink = new OutputStreamBitSink(out_stream);
		
		long training = System.nanoTime()-start;
		System.out.println("Training Time: " +((training/e9)/60)+"m "+((training/e9)%60)+"s");

		LZWApp.encodeArray(compressed, encoder, bit_sink, symbols);
		System.out.println("Array Encoded");
		long encoding = System.nanoTime()-start;
		System.out.println("Encoding Time: " +((encoding/e9)/60)+"m "+((encoding/e9)%60)+"s");

		
		//message.close();
		encoder.close(bit_sink);
		out_stream.close();

		BitSource bit_source = new InputStreamBitSource(new FileInputStream(out_file));
		OutputStream decoded_file = new FileOutputStream(new File("/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/16"+ base + "-decoded-"+vto+".dat"));
		//OutputStream decoded_file = new FileOutputStream(new File("/Users/oqbrennw/Desktop/COMP590/RawVideoSamples/" + base +"/"+ base + "-decoded16.dat"));

		
		//		SymbolDecoder decoder = new HuffmanDecoder(encoder.getCodeMap());
		SymbolDecoder decoder = new ArithmeticDecoder(model);
		int [] decodedList = LZWApp.decodeArray(decoder, bit_source, compressed.length);
		System.out.println("Array Decoded");
		System.out.println("Array Equal: "+Arrays.equals(compressed, decodedList));
		long decoding = System.nanoTime()-start;
		System.out.println("Decoding Time: " +((decoding/e9)/60)+"m "+((decoding/e9)%60)+"s");
		compressed = null;
		//LZWApp.decompressAndOutputLZW(decodedList, decoded_file,tbit);
		ArrayList<Byte> videoList = LZWApp.decompressLZWVideo(decodedList, tbit);
		long decompression = System.nanoTime()-start;
		System.out.println("Decompression Time: " +((decompression/e9)/60)+"m "+((decompression/e9)%60)+"s");
		LZWApp.outputDecompressedVideo(decoded_file, videoList, order, width, height, num_frames);
		decoded_file.close();
		//long decompression = System.nanoTime()-encoding;
		long runtime = System.nanoTime()-start;
		System.out.println("Output Time: " +((runtime/e9)/60)+"m "+((runtime/e9)%60)+"s");
	
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
		dict = null; 
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
	
	private static void trainModelWithArray(Unsigned12BitModel 
			//Unsigned14BitModel
			//Unsigned16BitModel 
			//Unsigned24BitModel 
			model
			, int[] compressed) {
		for(int i=0; i<compressed.length; i++){
			int k = compressed[i];
			model.train(k);
		}	
	}
	
	public static void encodeArray(int [] arr, SymbolEncoder encoder, BitSink bit_sink, Symbol[] symbols) 
			throws IOException {
		for(int i=0; i<arr.length; i++){
			encoder.encode(symbols[arr[i]], bit_sink);
		}
	}
	
	private static int [] decodeArray(SymbolDecoder decoder, BitSource bit_source, int size) 
			throws InsufficientBitsLeftException, IOException {
		ArrayList<Integer> res = new ArrayList<Integer>();
		for(int i=0; i<size; i++){
			res.add((
					(Unsigned12BitSymbol) 
					//(Unsigned14BitSymbol)
					//(Unsigned16BitSymbol)
					//(Unsigned24BitSymbol)
					decoder.decode(bit_source)).getValue());
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
	
	private static ArrayList<Byte> decompressLZWVideo(int[] list,int limit) throws IOException {
		ArrayList<Byte> video = new ArrayList<Byte>();
		HashMap<Integer,ArrayList<Byte>> dict = new HashMap<Integer,ArrayList<Byte>>();
		for(int i=0; i<256; i++){
			ArrayList<Byte> blist = new ArrayList<Byte>();
			blist.add(Byte.valueOf((byte)i));
			dict.put(i,blist);
		}
		ArrayList<Byte> w = new ArrayList<Byte>();
		w.addAll(dict.get((list[0])));
		for(Byte b: w){
			video.add(b);
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
				video.add(b);
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
	
	private static void outputDecompressedVideo(OutputStream out, ArrayList<Byte> videoList,VideoTraversalOrder order, int width,int height, int num_frames) throws IOException{
		byte[][][] video = new byte[width][height][num_frames];
		int count = 0;
		switch(order){
			case WIDTH_HEIGHT_FRAME:
				for(int i=0; i<num_frames; i++){
					for(int j=0; j<height; j++){
						for(int k=0; k<width; k++){
							video[k][j][i] = videoList.get(count++);
						}
					}
				}
				break;
			case WIDTH_FRAME_HEIGHT:
				for(int i=0; i<height; i++){
					for(int j=0; j<num_frames; j++){
						for(int k=0; k<width; k++){
							video[k][i][j] = videoList.get(count++);
						}
					}
				}
				break;
			case HEIGHT_WIDTH_FRAME:
				for(int i=0; i<num_frames; i++){
					for(int j=0; j<width; j++){
						for(int k=0; k<height; k++){
							video[j][k][i] = videoList.get(count++);
						}
					}
				}
				break;
			case HEIGHT_FRAME_WIDTH:
				for(int i=0; i<width; i++){
					for(int j=0; j<num_frames; j++){
						for(int k=0; k<height; k++){
							video[i][k][j] = videoList.get(count++);
						}
					}
				}
				break;
			case FRAME_HEIGHT_WIDTH:
				for(int i=0; i<width; i++){
					for(int j=0; j<height; j++){
						for(int k=0; k<num_frames; k++){
							video[i][j][k] = videoList.get(count++);
						}
					}
				}
				break;
			case FRAME_WIDTH_HEIGHT:
				for(int i=0; i<height; i++){
					for(int j=0; j<width; j++){
						for(int k=0; k<num_frames; k++){
							video[j][i][k] = videoList.get(count++);
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


