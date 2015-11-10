package net.vinote.smart.socket.protocol;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.net.ProtocolException;
import java.nio.ByteBuffer;

import net.vinote.smart.socket.exception.DecodeException;

/**
 * 数据报文的存储实体
 *
 * @author Seer
 * @version DataEntry.java, v 0.1 2015年8月28日 下午4:33:59 Seer Exp.
 */
public abstract class DataEntry implements Cloneable {

	/** 完整的数据流 */
	private ByteBuffer data;

	/** 当前数据存储区处于的操作模式 */
	private MODE mode;

	public static final int DEFAULT_DATA_LENGTH = 1024;

	/**
	 * 读取一个布尔值
	 *
	 * @return
	 */
	public final boolean readBoolen() {
		assertMode(MODE.READ);
		return readByte() == 1;
	}

	/**
	 * 从数据块中当前位置开始读取一个byte长度的整形值
	 *
	 * @return
	 */
	public final byte readByte() {
		assertMode(MODE.READ);
		return data.get();
	}

	/**
	 * 从数据块中当前位置开始读取一个byte数值
	 *
	 * @return
	 */
	public final byte[] readBytes() {
		int size = readInt();
		if (size < 0) {
			return null;
		}
		byte[] bytes = new byte[size];
		data.get(bytes);
		return bytes;
	}

	/**
	 * 从数据块中反序列化对象
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T> T readObjectByProtobuf() {
		byte[] bytes = readBytes();
		if (bytes == null) {
			return null;
		}
		SerializableBean bean = new SerializableBean();
		Schema<SerializableBean> schema = RuntimeSchema.getSchema(SerializableBean.class);
		ProtobufIOUtil.mergeFrom(bytes, bean, schema);
		return (T) bean.getBean();
	}

	/**
	 * 从数据块中当前位置开始读取一个int长度的整形值
	 *
	 * @return
	 */
	public final int readInt() {
		assertMode(MODE.READ);
		return data.getInt();
	}

	/**
	 * 重数据块中读取一个short长度的整形值
	 *
	 * @return
	 */
	public final short readShort() {
		assertMode(MODE.READ);
		return data.getShort();
	}

	/**
	 * 输出布尔值
	 *
	 * @param flag
	 */
	public final void writeBoolean(boolean flag) {
		writeByte(flag ? (byte) 1 : 0);
	}

	/**
	 * 往数据块中输入byte数值
	 *
	 * @param i
	 */
	public final void writeByte(byte i) {
		ensureCapacity(1);
		data.put(i);
	}

	/**
	 * 将对象进行序列化输出
	 *
	 * @param object
	 */
	public final <T> void writeObjectByProtobuf(T object) {
		Schema<SerializableBean> schema = RuntimeSchema.getSchema(SerializableBean.class);
		// 缓存buff
		LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
		// 序列化成protobuf的二进制数据
		SerializableBean bean = new SerializableBean();
		bean.setBean(object);
		byte[] data = ProtobufIOUtil.toByteArray(bean, schema, buffer);
		writeBytes(data);
	}

	/**
	 * 往数据块中输出byte数组
	 *
	 * @param data
	 */
	public final void writeBytes(byte[] data) {
		if (data != null) {
			writeInt(data.length);
			ensureCapacity(data.length);
			this.data.put(data);
		} else {
			writeInt(-1);
		}
	}

	/**
	 * 往数据块中输入int数值
	 *
	 * @param i
	 */
	public final void writeInt(int i) {
		assertMode(MODE.WRITE);
		ensureCapacity(4);
		data.putInt(i);
	}

	/**
	 * 往数据块中输入short数值
	 *
	 * @param i
	 */
	public final void writeShort(short i) {
		assertMode(MODE.WRITE);
		ensureCapacity(2);
		data.putShort(i);
	}

	protected final void reset(MODE mode) {
		this.mode = mode;
		ensureCapacity(0);
		data.position(0);
	}

	/**
	 * 输出字符串至数据体,以0x00作为结束标识符
	 *
	 * @param str
	 */
	public final void writeString(String str) {
		assertMode(MODE.WRITE);
		if (str == null) {
			str = "";
		}
		byte[] bytes = str.getBytes();
		ensureCapacity(1 + bytes.length);
		data.put(bytes);
		data.put((byte) 0x00);
	}

	/**
	 * 从数据块的当前位置开始读取字符串
	 *
	 * @return
	 */
	public final String readString() {
		assertMode(MODE.READ);
		int curIndex = data.position();
		while (data.get() != 0x00) {
			;
		}
		byte[] str = new byte[data.position() - curIndex];
		data.position(curIndex);
		data.get(str);
		return new String(str, 0, str.length - 1);
	}

	/**
	 * 若当前数据体处于read模式,则直接获取data,否则从临时数据区writeData拷贝至data中再返回
	 * <p>
	 * 在read模式下请确保已经完成了解密才可调用getData方法，否则会造成未完成解码的数据丢失
	 *
	 * @return
	 */
	public final ByteBuffer getData(boolean compress) {
		if (compress && data.limit() < data.capacity()) {
			data.flip();
			data = ByteBuffer.allocate(data.limit()).put(data);
		}
		return data;
	}

	public final ByteBuffer getData() {
		return getData(true);
	}

	public final void setData(ByteBuffer data) {
		this.data = data;
	}

	/**
	 * 断言当前数据库所处的模式为read or write
	 *
	 * @param mode
	 */
	private final void assertMode(MODE mode) {
		if (mode != this.mode) {
			throw new RuntimeException("current mode is " + this.mode + ", can not " + mode);
		}
	}

	/**
	 * 确保足够的存储容量
	 *
	 * @param minCapacity
	 */
	private void ensureCapacity(int minCapacity) {
		if (data == null) {
			data = ByteBuffer.allocate(Math.max(minCapacity, DEFAULT_DATA_LENGTH));
			data.limit(minCapacity);
		}
		if (data.capacity() - data.position() < minCapacity) {
			data = ByteBuffer.allocate(data.capacity() * 3 / 2 + 1).put(data);
		}
		int limit = data.position() + minCapacity;
		if (limit > data.limit()) {
			data.limit(limit);
		}
	}

	public abstract ByteBuffer encode() throws ProtocolException;

	public abstract void decode() throws DecodeException;

	/**
	 * 定位至数据流中的第n+1位
	 *
	 * @param n
	 */
	public final void position(int n) {
		if (data == null || data.capacity() < n) {
			ensureCapacity(n + 1);
		}
		if (data.limit() < n) {
			data.limit(n);
		}
		data.position(n);
	}

	/**
	 * 当前游标位置
	 */
	public final int getPosition() {
		return data.position();
	}

	/**
	 * 清除限制
	 */
	public void clearLimit() {
		data.limit(data.capacity());
	}

	public enum MODE {
		READ, WRITE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}

}
