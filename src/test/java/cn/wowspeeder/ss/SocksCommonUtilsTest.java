package cn.wowspeeder.ss;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

public class SocksCommonUtilsTest {

	@Test
	public void testIntToIp() {
		Assert.assertEquals("11.8.0.8",
			SocksCommonUtils.intToIp(185_073_672));
	}

	@Test
	public void testIpv6toCompressedForm() {
		byte[] bytes = new byte[]{
			1, 2, 4, 8, 16, 32, 64, 127, -1, -2,
			-4, -16, -32, -64, -128, 0};

		Assert.assertEquals(
			"102:408:1020:407f:fffe:fcf0:e0c0:8000",
			SocksCommonUtils.ipv6toCompressedForm(bytes)
		);
	}

	@Test
	public void testIpv6toCompressedFormCompressionApplied() {
		byte[] bytes = new byte[]{
			0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 2, 4, 8, 16, 32};

		Assert.assertEquals(
			"0::2:408:1020",
			SocksCommonUtils.ipv6toCompressedForm(bytes)
		);
	}

	@Test
	public void testIpv6toStr() {
		byte[] bytes = new byte[]{
			0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 2, 4, 8, 16, 32};

		Assert.assertEquals(
			"0:0:0:0:0:2:408:1020",
			SocksCommonUtils.ipv6toStr(bytes));
	}

	@Test
	public void testReadUsAscii() {
		final ByteBuf buffer = Unpooled.copiedBuffer(
			new byte[]{'f', 'o', 'o'});

		Assert.assertEquals(
			"foo",
			SocksCommonUtils.readUsAscii(buffer, 3)
		);
	}
}
