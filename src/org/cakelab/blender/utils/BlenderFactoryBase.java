package org.cakelab.blender.utils;

import static org.cakelab.blender.typemap.CFacadeMembers.__DNA__SDNA_INDEX;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.cakelab.blender.io.BlenderFile;
import org.cakelab.blender.io.FileHeader;
import org.cakelab.blender.io.block.Block;
import org.cakelab.blender.io.block.BlockTable;
import org.cakelab.blender.io.dna.internal.StructDNA;
import org.cakelab.blender.io.util.BigEndianInputStreamWrapper;
import org.cakelab.blender.io.util.Identifier;
import org.cakelab.blender.nio.CArrayFacade;
import org.cakelab.blender.nio.CFacade;
import org.cakelab.blender.nio.CPointer;


/**
 * This is the base class for generated factory classes of a specific blender 
 * version.
 * <p>
 * Factory classes are part of the utils package and optional. This base class 
 * contains the public factory methods to create blocks for facades, arrays and 
 * pointers. It also contains utilities to create a new blender files in a
 * derived factory class.
 * </p>
 * 
 * @author homac
 *
 */
public class BlenderFactoryBase {
	protected static class BlenderFileImplBase extends BlenderFile {

		protected BlenderFileImplBase(File file, StructDNA sdna, int blenderVersion) throws IOException {
			super(file, sdna, blenderVersion, null);
		}
		
	}
	/**
	 * This class provides access to the StructDNA image stored in each
	 * data model generated by Java Blend. This is a convenient way to
	 * create a StructDNA block without parsing the data model.
	 * @see BlenderFactoryBase#createStructDNA(String)
	 * @author homac
	 *
	 */
	protected static class StructDNAImage extends BlenderFile {
		protected StructDNAImage(BigEndianInputStreamWrapper in) throws IOException {
			super();
			this.io = in;
			header = new FileHeader();
			try {
				try {
					header.read(in);
					firstBlockOffset = in.offset();
					readStructDNA();
					in.close();
					
				} catch (IOException e) {
					throw new IOException("Error reading sdna image file", e);
				}
			} finally {
				try {in.close();} catch (Throwable suppress){}
			}
		}

	}

	/** Blender file associated with this instance of a factory. */
	protected BlenderFile blend;
	/** Null pointer associated with the blender file. */
	private CPointer<Object> NULL;
	
	/**
	 * Creates a new factory associated with the given blender file.
	 * 
	 * All blocks instantiated by this factory will be allocated in the
	 * given blender file.
	 * 
	 * @param blend
	 * @throws IOException
	 */
	public BlenderFactoryBase(BlenderFile blend) throws IOException {
		this.blend = blend;
		this.NULL = getNullPointer(blend);
	}


	
	/**
	 * Create a null pointer object associated with the given blender file.
	 * Since pointers have architecture specific properties, they have to be associated
	 * with a blender file (which defines the encoding).
	 * @return Null pointer object.
	 */
	public static CPointer<Object> getNullPointer(BlenderFile blend) throws IOException {
		// XXX: null pointer initialising with null block?
		return new CPointer<Object>(0, new Class[]{Object.class}, null, blend.getBlockTable());
	}
	
	/**
	 * Returns a null pointer object associated with the blender file of this factory.
	 * Since pointers have architecture specific properties, they have to be associated
	 * with a blender file (which defines the encoding).
	 * @return Null pointer object.
	 */
	public CPointer<Object> getNullPointer() {
		return NULL;
	}
	/**
	 * Allocate a new block for one instance of a C struct.
	 * <p>
	 * This method allocates a new block in the given blender file
	 * and assigns it to a facet of the given class (facetClass).
	 * </p>
	 * <b>Example:</b>
	 * <pre>
	 * BlenderFile blend = BlenderFactory.newBlenderFile(new File("my.blend"));
	 * Scene scene = BlenderFactory.newDNAStructBlock(BlockCodes.CODE_SCE, Scene.class, blend);
	 * </pre>
	 */
	@SuppressWarnings("unchecked")
	public static <T extends CFacade> T newCStructBlock(Identifier blockCode, Class<T> facetClass, BlenderFile blend) throws IOException {
		BlockTable blockTable = blend.getBlockTable();
		try {
			Field field__dna__sdnaIndex = facetClass.getDeclaredField(__DNA__SDNA_INDEX);
			int sdnaIndex = field__dna__sdnaIndex.getInt(null);
			Block block = blockTable.allocate(blockCode, CFacade.__io__sizeof(facetClass, blend.getEncoding().getAddressWidth()), sdnaIndex, 1);
			blend.add(block);
			return (T)CFacade.__io__newInstance(facetClass, block.header.getAddress(), block, blockTable);
		} catch (IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new IOException(e);
		} catch (NoSuchFieldException e) {
			throw new IOException("you cannot instantiate pointers or arrays this way. Use the appropriate factory methods for the respective types instead.", e);
		}
	}
	
	/**
	 * Factory instance method equivalent to {@link #newCStructBlock(Identifier, Class, BlenderFile)}.
	 */
	public <T extends CFacade> T newCStructBlock(Identifier blockCode, Class<T> facetClass) throws IOException {
		return newCStructBlock(blockCode, facetClass, blend);
	}
	
	/**
	 * Allocate a new block for multiple instances of a C struct.
	 * <p>
	 * This method allocates a new block of apropriate size to fit 'count'
	 * instances of the given C struct in the given blender file
	 * and assigns it to an array facet of the given class type (facetClass).
	 * </p>
	 * <b>Example:</b>
	 * <pre>
	 * BlenderFile blend = BlenderFactory.newBlenderFile(new File("my.blend"));
	 * CArrayFacade&lt;Scene&gt; scene = BlenderFactory.newDNAStructBlock(BlockCodes.CODE_SCE, Scene.class, 2, blend);
	 * </pre>
	 */
	public static <T extends CFacade> CArrayFacade<T> newCStructBlock(Identifier blockCode, Class<T> facetClass, int count, BlenderFile blend) throws IOException {
		BlockTable blockTable = blend.getBlockTable();
		try {
			Field field__dna__sdnaIndex = facetClass.getDeclaredField(__DNA__SDNA_INDEX);
			int sdnaIndex = field__dna__sdnaIndex.getInt(null);
			Block block = blockTable.allocate(blockCode, CFacade.__io__sizeof(facetClass, blend.getEncoding().getAddressWidth()), sdnaIndex, count);
			blend.add(block);
			return new CArrayFacade<T>(block.header.getAddress(), new Class[]{facetClass}, new int[]{count}, block, blockTable);
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
			throw new IOException(e);
		} catch (NoSuchFieldException e) {
			throw new IOException("you cannot instantiate pointers or arrays this way. Use the appropriate factory methods for the respective types instead.", e);
		}
	}

	/**
	 * Factory instance method equivalent to {@link #newCStructBlock(Identifier, Class, int, BlenderFile)}.
	 */
	public <T extends CFacade> CArrayFacade<T> newCStructBlock(Identifier blockCode, Class<T> facetClass, int count) throws IOException {
		return newCStructBlock(blockCode, facetClass, count, blend);
	}
	
	/**
	 * Allocate a new block for one instance of a <b>one-dimensional</b> array of 
	 * any <b>non-pointer</b> component type which is either a scalar or a DNA struct.
	 * <p>
	 * This method allocates a new block of appropriate size to fit an array of 
	 * the given arrayLength and the given componentType in the given blender file
	 * and assigns it to an array facet.
	 * </p>
	 * <b>Example:</b>
	 * <pre>
		// 1-dim float array with 4 elems
		CArrayFacade&lt;Float&gt; rgba = BlenderFactory.newDNAArrayBlock(BlockCodes.CODE_DATA, Float.class, 4, blend);
	 * </pre>
	 * 
	 * @param blockCode Code of the new block. Frequently ID_DATA for arrays.
	 * @param componentType Component type of the array.
	 * @param arrayLength length of the array
	 * @param blend blender file to add block to.
	 * @return CArrayFacade facet to access array data in the new block.
	 * @throws IOException
	 */
	public static <T> CArrayFacade<T> newCArrayBlock(Identifier blockCode, Class<T> componentType, int arrayLength, BlenderFile blend) throws IOException {
		if (CFacade.__io__subclassof(componentType, CArrayFacade.class)) {
			throw new IOException("Multi-dimensional arrays have to be instantiated giving all component types of the embedded arrays and their lengths.");
		} else if (CFacade.__io__subclassof(componentType, CPointer.class)) {
			throw new IOException("Arrays of pointers have to be instantiated giving all types of the pointer, too.");
		} else {
			int[] dimensions = new int[]{arrayLength};
			Class<?>[] typeList = new Class<?>[]{componentType};
			return newCArrayBlock(blockCode, typeList, dimensions, blend);
		}
	}
	/**
	 * Factory instance method equivalent to {@link #newCArrayBlock(Identifier, Class, int, BlenderFile)}.
	 */
	public <T> CArrayFacade<T> newCArrayBlock(Identifier blockCode, Class<T> componentType, int arrayLength) throws IOException {
		return newCArrayBlock(blockCode, componentType, arrayLength, blend);
	}
	/**
	 * Allocate a new block for one instance of a <b>multi-dimensional</b> array of 
	 * any component type supported by blender.
	 * <p>
	 * This method allocates a new block of appropriate size to fit an array of 
	 * the given arrayLength and the given component type specification (typeList) 
	 * and the given lengths for each dimension, in the given blender file and 
	 * assigns it to an array facade. Refer to {@link CArrayFacade} and {@link CPointer} 
	 * to understand the concept of type specifications by type lists.
	 * </p>
	 * <b>Example:</b>
	 * <pre>
	 *	// 2-dim 4x4 float array
	 *	Class&lt;?&gt;[] typeList = new Class[]{CArrayFacade.class, Float.class};
	 *	int[] dimensions = new int[]{4,4};
	 *	CArrayFacade&lt;CArrayFacade&lt;Float&gt;&gt; mv_mat4x4 = BlenderFactory.newDNAArrayBlock(BlockCodes.CODE_DATA, typeList, dimensions, blend);
	 *	
	 *	// 1-dim array of pointers on bytes (e.g. strings)
	 *	typeList = new Class[]{CArrayFacade.class, CPointer.class, Byte.class};
	 *	dimensions = new int[]{4};
	 *	CArrayFacade&lt;CPointer&lt;Byte&gt;&gt; fileList = BlenderFactory.newDNAArrayBlock(BlockCodes.CODE_DATA, typeList, dimensions, blend);
	 * </pre>
	 * @param blockCode Code of the new block. Frequently ID_DATA for arrays.
	 * @param typeList type specification for all referenced types
	 * @param dimensions length of each array dimension
	 * @param blend blender file to add block to.
	 */
	public static <T> CArrayFacade<T> newCArrayBlock(Identifier blockCode, Class<?>[] typeList, int[] dimensions, BlenderFile blend) throws IOException {
		BlockTable blockTable = blend.getBlockTable();
		int sdnaIndex = 0; // not a struct
		Class<?> elementaryType = typeList[dimensions.length-1];
		long size = CArrayFacade.__io__sizeof(elementaryType, dimensions, blend.getEncoding());
		Block block = blockTable.allocate(blockCode, size);
		block.header.setCount(dimensions[0]);
		block.header.setSdnaIndex(sdnaIndex);
		blend.add(block);
		return new CArrayFacade<T>(block.header.getAddress(), typeList, dimensions, block, blockTable);
	}
	/**
	 * Factory instance method equivalent to {@link #newCArrayBlock(Identifier, Class[], int[], BlenderFile)}.
	 */
	public <T> CArrayFacade<T> newCArrayBlock(Identifier blockCode, Class<?>[] typeList, int[] dimensions) throws IOException {
		return newCArrayBlock(blockCode, typeList, dimensions, blend);
	}
	/**
	 * This method creates a block with a single pointer in it. The returned pointer
	 * is a pointer on this created pointer. To change the value of that pointer use
	 * the method {@link CPointer#set(Object)} of the pointer on the pointer.
	 * 
	 * @param blockCode
	 * @param typeList
	 * @param blend blender file to add block to.
	 * @return a pointer on the pointer stored in the block.
	 * @throws IOException
	 */
	public static <T> CPointer<CPointer<T>> newCPointerBlock(Identifier blockCode, Class<?>[] typeList, BlenderFile blend) throws IOException {
		BlockTable blockTable = blend.getBlockTable();
		int sdnaIndex = 0; // not a struct
		int count = 1;
		int size = blend.getEncoding().getAddressWidth();
		Block block = blockTable.allocate(blockCode, size);
		block.header.setCount(count);
		block.header.setSdnaIndex(sdnaIndex);
		blend.add(block);
		Class<?>[] typeListExtended = new Class<?>[typeList.length+1];
		System.arraycopy(typeList, 0, typeListExtended, 1, typeList.length);
		typeListExtended[0] = CPointer.class;
		return new CPointer<CPointer<T>>(block.header.getAddress(), typeList, block, blockTable);
	}
	
	/**
	 * Factory instance method equivalent to {@link #newCPointerBlock(Identifier, Class[], BlenderFile)}.
	 */
	public <T> CPointer<CPointer<T>> newCPointerBlock(Identifier blockCode, Class<?>[] typeList) throws IOException {
		return newCPointerBlock(blockCode, typeList, blend);
	}
	/**
	 * This method creates a block with a set of pointers and returns an array facet to 
	 * access them. To change the value of the pointers use for example
	 * the method {@link CArrayFacade#set(int, Object)} of the pointer on the pointer.
	 * 
	 * @param blockCode Usually ID_DATA for lists of pointers.
	 * @param typeList type specification of the pointer.
	 * @param count number of pointers to fit in block.
	 * @param blend blender file to add block to.
	 * @throws IOException
	 */
	public static <T> CArrayFacade<CPointer<T>> newCPointerBlock(Identifier blockCode, Class<?>[] typeList, int count, BlenderFile blend) throws IOException {
		BlockTable blockTable = blend.getBlockTable();
		int sdnaIndex = 0; // not a struct
		int size = blend.getEncoding().getAddressWidth();
		Block block = blockTable.allocate(blockCode, size);
		block.header.setCount(count);
		block.header.setSdnaIndex(sdnaIndex);
		blend.add(block);
		Class<?>[] typeListExtended = new Class<?>[typeList.length+1];
		System.arraycopy(typeList, 0, typeListExtended, 1, typeList.length);
		typeListExtended[0] = CPointer.class;
		return new CArrayFacade<CPointer<T>>(block.header.getAddress(), typeList, new int[]{count}, block, blockTable);
	}
	
	/**
	 * Factory instance method equivalent to {@link #newCPointerBlock(Identifier, Class[], int count, BlenderFile)}.
	 */
	public <T> CArrayFacade<CPointer<T>> newCPointerBlock(Identifier blockCode, Class<?>[] typeList, int count) throws IOException {
		return newCPointerBlock(blockCode, typeList, count, blend);
	}
	/**
	 * This method provides the StructDNA from a given sdna.blend 
	 * image resource path. Every generated Java Blend data model
	 * has such an sdnaImage resource (to be found in "your/package/name/utils/resources/sdna.blend") 
	 * which contains just the sdna meta data of the blender version the
	 * model was generated from.
	 * 
	 * @param resourcePathTo_sdna_blend "your/package/name/utils/resources/sdna.blend"
	 */
	protected static StructDNA createStructDNA(String resourcePathTo_sdna_blend) throws IOException {
		InputStream in = BlenderFactoryBase.class.getClassLoader().getResourceAsStream(resourcePathTo_sdna_blend);
		StructDNAImage sdnaImage = new StructDNAImage(new BigEndianInputStreamWrapper(in, 8));
		StructDNA sdna = sdnaImage.getStructDNA();
		sdnaImage.close();
		return sdna;
	}
}
