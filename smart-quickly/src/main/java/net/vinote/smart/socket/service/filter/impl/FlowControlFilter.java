package net.vinote.smart.socket.service.filter.impl;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 流量控制过滤器
 *
 * @author Seer
 * @version FlowControlFilter.java, v 0.1 2015年4月11日 下午4:29:24 Seer Exp.
 */
public class FlowControlFilter implements SmartFilter {
	/** 流控计数器标识 */
	private static final String FLOW_CONTROL_FLAG = "_FLOW_CONTROL_FLAG_";

	@Override
	public void writeFilter(TransportSession session, ByteBuffer d) {
		AtomicInteger counter = getCounter(session);
		if (session.getQuickConfig().isServer()) {
			int num = counter.decrementAndGet();
			if (num * 1.0 / session.getQuickConfig().getCacheSize() < 0.382) {
				session.resumeReadAttention();
			}
		}
	}

	private AtomicInteger getCounter(TransportSession session) {
		AtomicInteger counter = session.getAttribute(FLOW_CONTROL_FLAG);
		if (counter == null) {
			counter = new AtomicInteger();
			session.setAttribute(FLOW_CONTROL_FLAG, counter);
		}
		return counter;
	}

	@Override
	public void processFilter(Session session, DataEntry d) {
	}

	@Override
	public void readFilter(TransportSession session, ByteBuffer d) {
		AtomicInteger counter = getCounter(session);
		if (session.getQuickConfig().isServer()) {
			int count = counter.incrementAndGet();
			if (count * 1.0 / session.getQuickConfig().getCacheSize() > 0.618) {
				session.pauseReadAttention();
			}
		}
	}

	@Override
	public void receiveFailHandler(TransportSession session, ByteBuffer d) {
	}

}
