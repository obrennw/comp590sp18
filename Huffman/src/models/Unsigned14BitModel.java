package models;

import java.io.IOException;
import java.io.InputStream;

public class Unsigned14BitModel implements SourceModel {

	public class Unsigned14BitSymbol implements Symbol {

		private int _value;

		public Unsigned14BitSymbol(int value) {
			if (value < 0 || value > 16383) {
				throw new IllegalArgumentException("Value out of range");
			}
			_value = value;
		}

		public int getValue() {
			return _value;
		}
		
		@Override
		public int compareTo(Symbol o) {
			if (!(o instanceof Unsigned14BitSymbol)) {
				throw new IllegalArgumentException("Unsigned14BitSymbol only comparable to type of same");
			}
			Unsigned14BitSymbol other = (Unsigned14BitSymbol) o;
			if (other.getValue() > getValue()) {
				return -1;
			} else if (other.getValue() < getValue()) {
				return 1;
			} else {
				return 0;
			}
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Symbol)) {
				return false;
			}
			return (compareTo((Symbol) o) == 0);
		}
		
		@Override
		public int hashCode() {
			return getValue();
		}
		
		@Override
		public String toString() {
			return "" + getValue();
				
		}
		
	}
	
	public class Unsigned14BitSymbolModel implements SymbolModel {
		private Unsigned14BitSymbol _symbol;
		private long _count;
		private Unsigned14BitModel _model;
		
		public Unsigned14BitSymbolModel(int value, long init_count, Unsigned14BitModel unsigned14BitModel) {
			_symbol = new Unsigned14BitSymbol(value);
			_count = init_count;
			_model = unsigned14BitModel;
		}
		
		public void incrementCount() {
			_count++;
		}

		@Override
		public long getProbability(long precision) {
			return _count * precision / _model.getCountTotal();
		}

		@Override
		public Symbol getSymbol() {
			return _symbol;
		}

		public long getCount() {
			return _count;
		}
	}
	
	private Unsigned14BitSymbolModel[] _values;
	private long _count_total;

	public Unsigned14BitModel() {
		_values = new Unsigned14BitSymbolModel[16384];
		for (int v=0; v<16384; v++) {
			_values[v] = new Unsigned14BitSymbolModel(v, 1, this);
		}
		_count_total = 16384;
	}
	
	public Unsigned14BitModel(long[] counts) {
		_values = new Unsigned14BitSymbolModel[16384];
		_count_total = 0;
		for (int v=0; v<16384; v++) {
			_values[v] = new Unsigned14BitSymbolModel(v, counts[v], this);
			_count_total += counts[v];
		}
	}

	public long getCountTotal() {
		return _count_total;
	}

	public void train(InputStream src, long input_count) 
			throws IOException
	{
		while (input_count > 0) {
			_values[src.read()].incrementCount();
			_count_total++;
			input_count--;
		}
	}
	
	public void train(int value) {
		_values[value].incrementCount();
		_count_total++;
	}

	@Override
	public int getSymbolCount() {
		return _values.length;
	}

	@Override
	public SymbolModel getByIndex(int i) {
		return _values[i];
	}
}

