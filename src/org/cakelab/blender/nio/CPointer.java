package org.cakelab.blender.nio;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.cakelab.blender.io.block.BlockTable;
import org.cakelab.blender.nio.CArrayFacade.CArrayFacadeIterator;

/**
 * Objects of this class represent a C pointer in Java. It provides
 * the following main functionalities:
 * <ul>
 * <li>pointer arithmetics</li>
 * <li>type casting</li>
 * </ul>
 * <h3>Pointer Type</h3>
 * A pointer in C is defined by its target type and the level
 * of indirection. For example <code>int** pint</code> is
 * said to be a pointer on a pointer of type <code>int</code>. 
 * Thus, the target type is <code>int</code> and the level of 
 * indirection is <code>2</code>.<br/>
 * In Java Blend a pointer is mapped to this template class where
 * the target type is provided through the template parameter <code>T</code>.
 * Thus an object of <code>CPointer&lt;CPointer&lt;Integer&gt;&gt;</code>
 * is the equivalent representation of <code>int**</code>.<br/>
 * 
 * <h3>CPointer Objects are Immutable</h3>
 * <p>
 * Objects of CPointer are immutable, that means you cannot change 
 * its address. Thus, CPointer objects behave like String's in Java:
 * Every 'modification' creates a copy of the object with that modification,
 * while the original objects stays unmodified. This has disadvantages in
 * performance, if you heavily depend on pointer arithmetics. Hence, there 
 * is another class called {@link CPointerMutable} which allows in-place
 * modification of pointers (similar to the relationship between String 
 * and StringBuffer).
 * </p>
 * 
 * <h3>Pointer Arithmetics</h3>
 * A pointer supports referencing (see {@link #get()}) and basic algebra
 * (see {@link #add(int)}). For advanced pointer arithmetics see 
 * {@link CPointerMutable}.
 * <h4>Referencing</h4>
 * Referencing the target <em>object</em> through
 * {@link #get()} returns that objects (Java) value. 
 * <ul>
 * <li>
 * If the target object
 * is of complex type (class), the value returned is a reference on an 
 * instance of a class derived from {@link CFacade}. That means, you get
 * access by reference: all modifications to the objects data will be 
 * reflected in the memory backing the object. This applies also to 
 * {@link CArrayFacade} objects but it applies not to instances of 
 * {@link CPointer} itself (see below).
 * </li>
 * <li>
 * If the target object is a scalar, then the value is the value read from 
 * the target address. That means, modifications to that value will not 
 * be reflected in the memory location of its origination.
 * </li>
 * </ul>
 * An object of type {@link CPointer} is a reference but treated as a scalar.
 * That means, if you receive a pointer from a method (e.g. from a facet, array 
 * or another pointer) than it is a copy - disconnected from its own memory location. Any
 * modification to the pointer is not reflected in its original memory location.
 * To assign a new address to its original memory location, you have to use the 
 * set method of the object, which provided you the pointer.<br/>
 * <h4>Example</h4>
 * <pre>
 * 
 * import static org.cakelab.blender.nio.CFacade.__dna__addressof;
 * 
 * CPointer<Link> next = link.getNext(); // retrieve address
 * Link anotherLink = .. ;                 // link we received elsewhere
 * link.setNext(__dna__addressof(anotherLink));  // assign new address to link.next
 * </pre>
 * <p>
 * See also {@link CPointerMutable} and {@link CFacade#__io__addressof(CFacade)}.
 * </p>
 * 
 * 
 * <h4>Basic Algebra</h4>
 * <p>
 * Typical pointer arithmetics are incrementing and decrementing the address
 * by the size of the target type. This actually reflects the same 
 * functionality provided by array types. CPointer provides different ways
 * to achieve this functionality: Conversion to an array (see Section 
 * Array Conversion below) and the method {@link #plus(int)}.
 * </p>
 * <p>Array conversion provides you either a Java array type or an iterable CArrayFacade
 * which is pretty straight forward to handle. The method {@link #plus(int)} increments
 * the address by the given increment multiplied by the size of the pointer 
 * target type. Thus you can use the pointer like an iterator as in the example below.
 * </p>
 * 
 * <h5>Example</h5>
 * <pre>
 * // iterating over a null terminated list of materials
 * for (CPointer<Material> pmat = .. ; 
 *      !pmat.isNull();                 // null check
 *      pmat = pmat.plus(+1))            // inc address by sizeof(Material)
 * {
 *   Material mat = pmat.get();
 * }
 * </pre>
 * <p>
 * Please note, that the result of {@link #plus(int)} has to be assigned 
 * to pmat, since {@link CPointer} is immutable and {@link #plus(int)} does
 * not change the address of the pointer itself (see also {@link CPointerMutable}).
 * </p>
 * <p>
 * This functionality of course requires that the pointer is of the correct
 * type (same as in C). Pointer arithmetics to a <code>void*</code> 
 * (which is mapped to <code>CPointer&lt;Object&gt;</code> are permitted.
 * Thus, you have to cast the pointer to the correct target type first 
 * (see Section Type Casts below).
 * </p>
 * 
 * <h3>Type Casts</h3>
 * Type casts are supported through the methods {@link #cast(Class)} and  
 * {@link #cast(Class[])}. Both take a parameter which describes the new
 * target type of the casted pointer. Since template parameters
 * are not available at runtime, type casts of pointers with multiple
 * indirections and pointers on arrays require you to provide even the 
 * types of the targeted types. For example, if you have a pointer
 * on a pointer of type Link, than the first pointer is of type pointer
 * on pointer and the second pointer is of type pointer on Link.
 * <h4>Example</h4>
 * <pre>
 * CPointer<CPointer<Link> pplink = .. ;
 * CPointer<CPointer<Scene> ppscene;
 * ppscene = pplink.cast(new Class[]{Pointer.class, Scene.class};
 * CPointer<Scene> pscene = ppscene.get();
 * Scene scene = pscene.get();
 * </pre>
 * <p>This can get confusing but you will need to cast pointers 
 * with multiple levels of indirection rather rare or never.</p>
 * 
 * <h3>Array Conversion</h3>
 * <p>
 * The method {@link #toArray(int)} returns an array with copies
 * of the values received from the address the pointer points to.
 * </p>
 * <p>
 * Since it is not possible to use scalar types (such as int, double etc.)
 * as template parameter, there are several different flavours of 
 * {@link #toArray(byte[], int, int)} and {@link #toByteArray(int)} 
 * for all scalar types. Please note, that there are two special methods
 * for int64, since long can refer to integer of 4 or 8 byte depending
 * on the data received from blender file. Refer to the originating 
 * facet, you received the pointer from, to determine its actual type.
 * </p>
 * @author homac
 *
 * @param <T> Target type of the pointer.
 */
public class CPointer<T> extends CFacade {
	
	/**
	 * Type of the target the pointer is able to address.
	 */
	protected Class<?>[] targetTypeList;
	protected long targetSize;
	private Constructor<T> constructor;
	
	
	CPointer(CPointer<T> other, long targetAddress) {
		super(other, targetAddress);
		this.targetTypeList = other.targetTypeList;
		this.targetSize = other.targetSize;
		this.constructor = other.constructor;
	}

	/**
	 * Copy constructor. It creates another instance of the 
	 * 'other' pointer.
	 * 
	 * @param other Pointer to be copied.
	 */
	public CPointer(CPointer<T> other) {
		this(other, other.__io__address);
	}
	
	
	@SuppressWarnings("unchecked")
	public CPointer(long targetAddress, Class<?>[] targetTypes, BlockTable memory) {
		super(targetAddress, memory);
		this.targetTypeList = (Class<T>[]) targetTypes;
		this.targetSize = __io__sizeof(targetTypes[0]);
	}
	
	/**
	 * @return Copy of the value the pointer points to.
	 * @throws IOException
	 */
	public T get() throws IOException {
		return __get(__io__address);
	}
	
	public T __get(long address) throws IOException {
		if (targetSize == 0) throw new ClassCastException("Target type is unspecified (i.e. void*). Use cast() to specify its type first.");
		if (isPrimitive(targetTypeList[0])) {
			return getScalar(address);
		} else if (targetTypeList[0].isArray()){
			throw new ClassCastException("Impossible type declaration containing a pointer on an array (Cannot be declared in C).");
		} else {
			return (T) getCFacade(address);
		}
	}
	
	public void set(T value) throws IOException {
		__set(__io__address, value);
	}
	
	protected void __set(long address, T value) throws IOException {
		if (isPrimitive(targetTypeList[0])) {
			setScalar(address, value);
		} else if (targetTypeList[0].equals(CPointer.class)) {
			CPointer<?> p = (CPointer<?>) value;
			__io__block.writeLong(address, p.__io__address);
		} else {
			// object or array
			
			if (__io__equals((CFacade)value, address)) {
				// this is a reference on the object, which is already inside the array
			} else if (__io__same__encoding(this, (CFacade)value)) {
				// we can perform a low level copy
				__io__native__copy(__io__block, address, (CFacade)value);
			} else {
				// we have to reinterpret data to convert to different encoding
				__io__generic__copy((CFacade)__get(address));
			}
		}
	}
	
	protected boolean isPrimitive(Class<?> type) {
		
		return type.isPrimitive() 
				|| type.equals(int64.class)
				|| type.equals(Byte.class)
				|| type.equals(Short.class)
				|| type.equals(Integer.class)
				|| type.equals(Long.class)
				|| type.equals(Float.class)
				|| type.equals(Double.class)
				;
	}


	/**
	 * This returns the address this pointer points to.
	 * 
	 * @return
	 * @throws IOException address the pointer points to
	 */
	public long getAddress() throws IOException {
		return __io__address;
	}
	
	/**
	 * Checks whether the address of the pointer equals null.
	 * @return
	 */
	public boolean isNull() {
		return __io__address == 0;
	}

	/**
	 * Tells whether the pointer points to actual data or in 
	 * a region of memory, which does not exist.
	 * Thus, it is a bit more than a null pointer check ({@link #isNull()} 
	 * and it is more expensive performance wise.<br/>
	 * 
	 * This method is mainly intended for debugging purposes.
	 * 
	 * @return true, if you can access the memory
	 */
	public boolean isValid() {
		return !isNull() && (__io__block != null && __io__block.contains(__io__address)); 
	}
	
	/**
	 * Type cast for pointers with just one indirection.
	 * 
	 * Casts the pointer to a different targetType.
	 * <pre>
	 * Pointer&lt;ListBase&gt; p; 
	 * ..
	 * Pointer &lt;Scene&gt; pscene = p.cast(Scene.class);
	 * </pre>
	 * 
	 * <h4>Attention!</h4>
	 * This is a very dangerous and error prone method since you can
	 * cast to anything. But you will need it several times.
	 * 
	 * @param type
	 * @return
	 */
	public <U> CPointer<U> cast(Class<U> type) {
		return new CPointer<U>(__io__address, new Class<?>[]{type}, __io__blockTable);
	}
	
	/**
	 * Type cast for pointers with multiple levels of indirection 
	 * (pointer on pointer). 
	 * Casts the pointer to a different targetType.
	 * <pre>
	 * CPointer&lt;CPointer&lt;ListBase&gt;&gt; p; 
	 * ..
	 * CPointer&lt;CPointer&lt;Scene&gt;&gt; pscene = p.cast(Scene.class);
	 * </pre>
	 * 
	 * <h4>Attention!</h4>
	 * This is an even more dangerous and error prone method than 
	 * {@link CPointer#cast(Class)} since you can do even more nasty stuff.
	 * 
	 * @param type
	 * @return
	 */
	public <U> CPointer<U> cast(Class<U>[] types) {
		return new CPointer<U>(__io__address, types, __io__blockTable);
	}

	/**
	 * Type cast for pointers with multiple levels of indirection 
	 * (pointer on pointer). 
	 * Casts the pointer to a different targetType.
	 * <pre>
	 * CPointer&lt;CPointer&lt;ListBase&gt;&gt; p; 
	 * ..
	 * CPointer&lt;CPointer&lt;Scene&gt;&gt; pscene = p.cast(Scene.class);
	 * </pre>
	 * 
	 * <h4>Attention!</h4>
	 * This is an even more dangerous and error prone method than 
	 * {@link CPointer#cast(Class)} since you can do even more nasty stuff.
	 * 
	 * @param type
	 * @return
	 * @throws IOException 
	 */
	public CArrayFacade<T> cast(CArrayFacade<T> type) throws IOException {
		if (this.getClass().equals(type)) {
			return (CArrayFacade<T>)this;
		} else {
			throw new IOException("not implemented");
		}
	}

	/**
	 * Converts the data referenced by the pointer into an Java array 
	 * of the given length.
	 * 
	 * @param length
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public T[] toArray(int length) throws IOException {
		T[] arr = (T[])Array.newInstance(targetTypeList[0], length);
		long address = __io__address;
		for (int i = 0; i < length; i++) {
			arr[i] = __get(address);
			address += targetSize;
		}
		return arr;
	}
	

	public CArrayFacade<T> toCArrayFacade(int len) {
		return new CArrayFacade<T>(__io__address, targetTypeList, new int[]{len}, __io__blockTable);
	}
	
	public byte[] toArray(byte[] data, int off, int len)
			throws IOException {
		if (!targetTypeList[0].equals(Byte.class)) throw new ClassCastException("cannot cast " + targetTypeList[0].getSimpleName() + " to " + data.getClass().getSimpleName() + ". You have to cast the pointer first.");
		__io__block.readFully(__io__address, data, off, len);
		return data;
	}

	public void fromArray(byte[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(Byte.class)) throw new ClassCastException("cannot cast " + data.getClass().getSimpleName() + " to " + targetTypeList[0].getSimpleName() + ". You have to cast the pointer first.");
		__io__block.writeFully(__io__address, data, off, len);
	}

	public byte[] toByteArray(int len)
			throws IOException {
		return toArray(new byte[len], 0, len);
	}

	public void fromArray(byte[] data) throws IOException {
		if (!targetTypeList[0].equals(Byte.class)) throw new ClassCastException("cannot cast " + data.getClass().getSimpleName() + " to " + targetTypeList[0].getSimpleName() + ". You have to cast the pointer first.");
		fromArray(data, 0, data.length);
	}
	

	public short[] toArray(short[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(Short.class)) throw new ClassCastException("cannot cast " + targetTypeList[0].getSimpleName() + " to " + data.getClass().getSimpleName() + ". You have to cast the pointer first.");
		__io__block.readFully(__io__address, data, off, len);
		return data;
	}
	
	public void fromArray(short[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(Short.class)) throw new ClassCastException("cannot cast " + data.getClass().getSimpleName() + " to " + targetTypeList[0].getSimpleName() + ". You have to cast the pointer first.");
		__io__block.writeFully(__io__address, data, off, len);
	}
	
	public short[] toShortArray(int len)
			throws IOException {
		return toArray(new short[len], 0, len);
	}

	public void fromArray(short[] data) throws IOException {
		fromArray(data, 0, data.length);
	}
	

	public int[] toArray(int[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(Integer.class)) throw new ClassCastException("cannot cast " + targetTypeList[0].getSimpleName() + " to " + data.getClass().getSimpleName() + ". You have to cast the pointer first.");
		__io__block.readFully(__io__address, data, off, len);
		return data;
	}

	public int[] toIntArray(int len)
			throws IOException {
		return toArray(new int[len], 0, len);
	}

	public void fromArray(int[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(Integer.class)) throw new ClassCastException("cannot cast " + data.getClass().getSimpleName() + " to " + targetTypeList[0].getSimpleName() + ". You have to cast the pointer first.");
		__io__block.writeFully(__io__address, data, off, len);
	}
	
	public void fromArray(int[] data) throws IOException {
		fromArray(data, 0, data.length);
	}
	

	public long[] toArray(long[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(Long.class)) throw new ClassCastException("cannot cast " + targetTypeList[0].getSimpleName() + " to " + data.getClass().getSimpleName() + ". You have to cast the pointer first.");
		__io__block.readFully(__io__address, data, off, len);
		return data;
	}

	public long[] toLongArray(int len)
			throws IOException {
		return toArray(new long[len], 0, len);
	}

	public void fromArray(long[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(Long.class)) throw new ClassCastException("cannot cast " + data.getClass().getSimpleName() + " to " + targetTypeList[0].getSimpleName() + ". You have to cast the pointer first.");
		__io__block.writeFully(__io__address, data, off, len);
	}
	
	public void fromArray(long[] data) throws IOException {
		fromArray(data, 0, data.length);
	}
	

	public long[] toArrayInt64(long[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(int64.class)) throw new ClassCastException("cannot cast " + targetTypeList[0].getSimpleName() + " to " + data.getClass().getSimpleName() + ". You have to cast the pointer first.");
		__io__block.readFullyInt64(__io__address, data, off, len);
		return data;
	}

	public long[] toInt64Array(int len)
			throws IOException {
		return toArray(new long[len], 0, len);
	}

	public void fromInt64Array(long[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(int64.class)) throw new ClassCastException("cannot cast " + data.getClass().getSimpleName() + " to " + targetTypeList[0].getSimpleName() + ". You have to cast the pointer first.");
		__io__block.writeFullyInt64(__io__address, data, off, len);
	}
	
	public void fromInt64Array(long[] data) throws IOException {
		fromArray(data, 0, data.length);
	}
	


	public float[] toArray(float[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(Float.class)) throw new ClassCastException("cannot cast " + targetTypeList[0].getSimpleName() + " to " + data.getClass().getSimpleName() + ". You have to cast the pointer first.");
		__io__block.readFully(__io__address, data, off, len);
		return data;
	}

	public float[] toFloatArray(int len)
			throws IOException {
		return toArray(new float[len], 0, len);
	}

	public void fromArray(float[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(Float.class)) throw new ClassCastException("cannot cast " + data.getClass().getSimpleName() + " to " + targetTypeList[0].getSimpleName() + ". You have to cast the pointer first.");
		__io__block.writeFully(__io__address, data, off, len);
	}
	
	public void fromArray(float[] data) throws IOException {
		fromArray(data, 0, data.length);
	}
	


	public double[] toArray(double[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(Double.class)) throw new ClassCastException("cannot cast " + targetTypeList[0].getSimpleName() + " to " + data.getClass().getSimpleName() + ". You have to cast the pointer first.");
		__io__block.readFully(__io__address, data, off, len);
		return data;
	}
	
	public double[] toDoubleArray(int len)
			throws IOException {
		return toArray(new double[len], 0, len);
	}

	public void fromArray(double[] data, int off, int len) throws IOException {
		if (!targetTypeList[0].equals(Double.class)) throw new ClassCastException("cannot cast " + data.getClass().getSimpleName() + " to " + targetTypeList[0].getSimpleName() + ". You have to cast the pointer first.");
		__io__block.writeFully(__io__address, data, off, len);
	}
	
	public void fromArray(double[] data) throws IOException {
		fromArray(data, 0, data.length);
	}
	


	/* ************************************************** */
	//                    PROTECTED
	/* ************************************************** */


	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected T getCFacade(long targetAddress) throws IOException {
		try {
			if (targetTypeList[0].equals(CPointer.class)) {
				long address = __io__block.readLong(targetAddress);
				return (T) new CPointer(address, Arrays.copyOfRange(targetTypeList, 1, targetTypeList.length), __io__blockTable);
			} else {
				return (T) CFacade.__io__newInstance((Class<? extends CFacade>) targetTypeList[0], targetAddress, __io__blockTable);
			}
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new IOException(e);
		}
	}



	@SuppressWarnings("unchecked")
	protected T getScalar(long address) throws IOException {
		Object result = null;
		
		Class<?> type = targetTypeList[0];
		
		if (type.equals(Byte.class) || type.equals(byte.class)) {
			result = __io__block.readByte(address);
		} else if (type.equals(Short.class) || type.equals(short.class)) {
			result = __io__block.readShort(address);
		} else if (type.equals(Integer.class) || type.equals(int.class)) {
			result = __io__block.readInt(address);
		} else if (type.equals(Long.class) || type.equals(long.class)) {
			result = __io__block.readLong(address);
		} else if (type.equals(int64.class)) {
			result = __io__block.readInt64(address);
		} else if (type.equals(Float.class) || type.equals(float.class)) {
			result = __io__block.readFloat(address);
		} else if (type.equals(Double.class) || type.equals(double.class)) {
			result = __io__block.readDouble(address);
		} else {
			throw new ClassCastException("unrecognized scalar type: " + type.getName());
		}
		return (T)result;
	}

	
	protected void setScalar(long address, T elem) throws IOException {
		Class<?> type = targetTypeList[0];
		
		if (type.equals(Byte.class) || type.equals(byte.class)) {
			__io__block.writeByte(address, (byte) elem);
		} else if (type.equals(Short.class) || type.equals(short.class)) {
			__io__block.writeShort(address, (short) elem);
		} else if (type.equals(Integer.class) || type.equals(int.class)) {
			__io__block.writeInt(address, (int) elem);
		} else if (type.equals(Long.class) || type.equals(long.class)) {
			__io__block.writeLong(address, (long) elem);
		} else if (type.equals(int64.class)) {
			__io__block.writeInt64(address, (long) elem);
		} else if (type.equals(Float.class) || type.equals(float.class)) {
			__io__block.writeFloat(address, (float) elem);
		} else if (type.equals(Double.class) || type.equals(double.class)) {
			__io__block.writeDouble(address, (double) elem);
		} else {
			throw new ClassCastException("unrecognized scalar type: " + type.getName());
		}
	}

	/**
	 * Creates a mutable pointer which allows to change its address in-place.
	 * @see CPointerMutable
	 * @return
	 */
	public CPointerMutable<T> mutable() {
		return new CPointerMutable<T>(this);
	}

	
	/**
	 * {@link #plus(int)} returns new instance with the result
	 * of the addition.
	 * 
	 * Allows (almost) equivalent handling as operator.
	 * <pre>
	 * int *p, *a;
	 * a = p = .. ;
	 * a = (p+1)+1;
	 * </pre>
	 * same as
	 * <pre>
	 * CPointerMutable<Integer> p, a;
	 * p = a = .. ;
	 * p = (p.plus(1)).plus(1);
	 * </pre>
	 * where the post condition
	 * <pre>
	 *  post condition: (p != a)
	 * </pre>
	 * holds.
	 * 
	 * @param value
	 * @return new instance of this pointer with an address+=targetSize
	 * @throws IOException
	 */
	public CPointer<T> plus(int value) throws IOException {
		return new  CPointer<T>(this, __io__address + targetSize);
	}

	/**
	 * Pointer comparison. 
	 * 
	 * This method provides pointer comparison functionality.
	 * It allows comparison to all objects derived from {@link CFacade}
	 * including pointers, arrays and iterators of both.
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj instanceof CArrayFacadeIterator) {
			return __io__address == ((CArrayFacadeIterator)obj).getCurrentAddress();
		}
		if (obj instanceof CFacade) {
			return ((CFacade) obj).__io__address == __io__address;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (int)((__io__address>>32) | (__io__address));
	}

	
}
