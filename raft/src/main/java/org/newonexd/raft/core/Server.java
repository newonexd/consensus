package org.newonexd.raft.core;

import com.google.gson.Gson;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.newonexd.raft.common.msg.Message;
import org.newonexd.raft.common.msg.MessageType;
import org.newonexd.raft.common.msg.RpcMessage;
import org.newonexd.raft.util.RpcApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.concurrent.*;

/**
 * @description server
 * @author newonexd
 * @date 2022/6/26 14:54
 */
public final class Server{
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private Thread thread;


    private final static ConcurrentHashMap<String,ChannelHandlerContext> channels = new ConcurrentHashMap<>();


    public void start(){
        thread = new Thread(this::init);
        thread.setDaemon(true);
        thread.start();
    }

    private void init(){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            String address = InetAddress.getLocalHost().getHostAddress();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) {
                            channel.pipeline()
                                    .addLast(new IdleStateHandler(0, 0, 30 * 3, TimeUnit.SECONDS))  // beat 3N, close if idle
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(5 * 1024 * 1024))
                                    .addLast(new RequestHandler());
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = bootstrap.bind(address,9999).sync();

            future.channel().closeFuture().sync();
        }catch (InterruptedException e){
            logger.info("Node Server Start fail,cause: {}",e.getMessage());
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }finally {
            try{
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }catch (Exception e){
                logger.error(e.getMessage(),e);
            }
        }
    }

    public void stop(){
        if(null != thread && thread.isAlive()){
            thread.interrupt();
        }
        logger.info("Node Server Stop success.");
    }

    /**
     * @description 向所有节点发送广播信息
     * @author newonexd
     * @date 2022/6/26 15:46
     */
    public void broadcast(){
        //TODO
        channels.values().forEach(ctx->writeResponse(ctx,new Message(null,null).toString()));
    }


    /**
     * write response
     */
    private static void writeResponse(ChannelHandlerContext ctx, String responseJson) {
        // write response
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8));   //  Unpooled.wrappedBuffer(responseJson)
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");       // HttpHeaderValues.TEXT_PLAIN.toString()
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }

    static class RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

        private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                0,
                200,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> new Thread(r, "Node Server RequestThreadPool-" + r.hashCode()),
                (r, executor) -> {
                    throw new RuntimeException("Node Server RequestThreadPool is EXHAUSTED!");
                });

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) {
            THREAD_POOL_EXECUTOR.execute(() -> {
                String response = process(fullHttpRequest);
                writeResponse(channelHandlerContext,response);
            });
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }



        private String process(FullHttpRequest fullHttpRequest){
            String requestData = fullHttpRequest.content().toString(CharsetUtil.UTF_8);
            Gson gson = new Gson();
            Message message = gson.fromJson(requestData, Message.class);


            if(message.getMessageType().equals(MessageType.REQUEST)){
                RpcMessage rpcMessage = (RpcMessage) message.getMessageBody();
                try {
                    Object object = RpcApplication.getComponentByClassName(rpcMessage.getClassName());

                    Class<?> clazz = object.getClass();
                    Method method = clazz.getDeclaredMethod(rpcMessage.getMethodName(),String.class);
                    return (String)method.invoke(object,rpcMessage.getArguments());
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    logger.warn("Invoke Method: {},failed,Reason: {} ",rpcMessage.getMethodName(),e.getMessage());
                }
            }
            return "";
        }
    }

}
