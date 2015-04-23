package com.ace.ng.codec.encrypt;


import com.ace.ng.codec.ByteCustomBuf;
import com.ace.ng.codec.CustomBuf;
import com.ace.ng.codec.OutputPacket;
import com.ace.ng.codec.binary.BinaryEncryptUtil;
import com.ace.ng.codec.binary.BinaryPacket;
import com.ace.ng.constant.VarConst;
import com.ace.ng.session.ISession;
import com.ace.ng.session.Session;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
/**
 * 网络数据报文编码器
 * @author Chenlong
 * 协议格式<br>
 *     <table  border frame="box">
 *         <tr>
 *             <th style="text-align:center"></th>
 *             <th style="text-align:center">包长</th>
 *             <th style="text-align:center">是否加密</th>
 *             <th style="text-align:center">密码表索引</th>
 *             <th style="text-align:center">指令ID(cmd)</th>
 *             <th style="text-align:center">消息错误码(code)</th>
 *             <th style="text-align:center">消息体(OutMessage中自定义内容)</th>
 *         </tr>
 *         <tr>
 *             <td>数据类型</td>
 *             <td style="text-align:center">short</td>
 *             <td style="text-align:center">byte</td>
 *             <td style="text-align:center">byte</td>
 *             <td style="text-align:center">short</td>
 *             <td style="text-align:center">byte</td>
 *             <td style="text-align:center">byte[]</td>
 *         </tr>
 *         <tr>
 *             <td>每部分字节数</td>
 *             <td style="text-align:center">2</td>
 *             <td style="text-align:center">1</td>
 *             <td style="text-align:center">1</td>
 *             <td style="text-align:center">2</td>
 *             <td style="text-align:center">1</td>
 *             <td style="text-align:center">根据消息体内容计算</td>
 *         </tr>
 *         <tr>
 *             <td style="text-align:center">是否加密</td>
 *             <td colspan="3" style="text-align:center">未加密部分</td>
 *             <td colspan="3" style="text-align:center">加密部分</td>
 *         </tr>
 *     </table>
 *
 * */
@Sharable
public class BinaryEncryptEncoder extends MessageToMessageEncoder<OutputPacket> {
    private static Logger logger= LoggerFactory.getLogger(BinaryEncryptEncoder.class);
    @Override
	protected void encode(ChannelHandlerContext ctx, OutputPacket output, List<Object> out)
			throws Exception {
		ByteBuf buf=PooledByteBufAllocator.DEFAULT.buffer();
		buf.writeShort(output.getCmd());
		buf.writeByte(output.getCode());
		CustomBuf content=new ByteCustomBuf(buf);
        output.getOutput().encode(content);
        byte[] dst=new byte[buf.readableBytes()];
        ISession session=ctx.channel().attr(Session.SESSION_KEY).get();
        Object isEncryptObject=session.getAttribute(Session.NEED_ENCRYPT);//是否需要加密
        boolean isEncrypt=false;
        if(isEncryptObject!=null){
            isEncrypt=(Boolean)isEncryptObject;
        }
        buf.readBytes(dst);
        int index=new Random().nextInt(256);//随机获取密码索引
        if(isEncrypt){
            List<Short> passports=session.getAttribute(Session.PASSPORT);
            short passport=passports.get(index);//根据索引获取密码
            String secretKey=ctx.attr(Session.SECRRET_KEY).get();
            BinaryEncryptUtil.encode(dst, dst.length, secretKey, passport);//加密
        }
        buf= PooledByteBufAllocator.DEFAULT.buffer(dst.length + 4);//开辟新Buff
        //buf.writeShort(dst.length + 2);//写入包长度
        buf.writeBoolean(isEncrypt);//写入加密标识
        buf.writeByte(index);//写入密码索引
        buf.writeBytes(dst);//写入dst
        BinaryPacket packet=new BinaryPacket(buf);
        out.add(packet);

	}
}
