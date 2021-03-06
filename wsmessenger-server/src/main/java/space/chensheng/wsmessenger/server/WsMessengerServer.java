package space.chensheng.wsmessenger.server;

import io.netty.channel.Channel;
import space.chensheng.wsmessenger.common.listener.LifecycleListenerManager;
import space.chensheng.wsmessenger.common.listener.MessageListenerManager;
import space.chensheng.wsmessenger.message.component.WsMessage;
import space.chensheng.wsmessenger.message.sysmsg.ResponseMessage;
import space.chensheng.wsmessenger.server.clientmng.ClientInfo;
import space.chensheng.wsmessenger.server.clientmng.ClientRegistry;
import space.chensheng.wsmessenger.server.clientmng.ClientValidator;
import space.chensheng.wsmessenger.server.component.ServerContext;
import space.chensheng.wsmessenger.server.listener.ServerLifecycleListener;
import space.chensheng.wsmessenger.server.listener.ServerMessageListener;

import java.util.List;

/**
 * Messenger server which can send message to messenger client through websocket protocol built upon netty.
 * @author sheng.chen
 */
public class WsMessengerServer extends MessengerServer { 
	private MessageListenerManager<ServerMessageListener<?>, WsMessage> msgListenerMgr = new MessageListenerManager<ServerMessageListener<?>, WsMessage>();

	private LifecycleListenerManager<ServerLifecycleListener> lifecycleListenerMgr = new LifecycleListenerManager<ServerLifecycleListener>();

	private ClientValidator clientValidator;
	
	WsMessengerServer() {
	}

	WsMessengerServer(ServerContext serverContext) {
	    super(serverContext);
    }
	
	@Override
	public void onMessage(WsMessage message, String senderId) {
		Channel channel = ClientRegistry.getInstance().findClient(senderId);
		ClientInfo clientInfo = ClientRegistry.resolveClientInfo(channel);
		if (clientInfo == null) {
			return;
		}
		
		sendResponseIfNecessary(message, clientInfo);
		
		List<ServerMessageListener<?>> targetListeners = msgListenerMgr.find(message);
		if (targetListeners == null || targetListeners.isEmpty()) {
			return;
		}
		
		for (ServerMessageListener<?> listener : targetListeners) {
			listener.handleMessage(message, clientInfo, this);
		}
	}

	@Override
	public void onClientConnect(ClientInfo clientInfo) {
		List<ServerLifecycleListener> listeners = lifecycleListenerMgr.findListeners();
		if (!listeners.isEmpty()) {
			for (ServerLifecycleListener listener : listeners) {
				listener.onClientConnect(clientInfo, this);
			}
		}
	}

	@Override
	public void onClientDisconnect(ClientInfo clientInfo) {
		List<ServerLifecycleListener> listeners = lifecycleListenerMgr.findListeners();
		if (!listeners.isEmpty()) {
			for (ServerLifecycleListener listener : listeners) {
				listener.onClientDisconnect(clientInfo, this);
			}
		}
	}

	@Override
	public void onStarted() {
		List<ServerLifecycleListener> listeners = lifecycleListenerMgr.findListeners();
		if (!listeners.isEmpty()) {
			for (ServerLifecycleListener listener : listeners) {
				listener.onServerStart(this);
			}
		}
	}

    @Override
    public boolean needToValidateClient() {
        return clientValidator != null;
    }

    @Override
    public boolean validateClient(ClientInfo clientInfo) {
	    if (clientValidator == null) {
	        return true;
        }

        return clientValidator.validate(clientInfo);
    }

    void addMessageListener(ServerMessageListener<?> listener) {
		msgListenerMgr.add(listener);
	}
	
	void addMessageListeners(List<ServerMessageListener<?>> listeners) {
		msgListenerMgr.add(listeners);
	}
	
	void addLifecycleListener(ServerLifecycleListener listener) {
		lifecycleListenerMgr.add(listener);
	}
	
	void addLifecycleListeners(List<ServerLifecycleListener> listeners) {
		lifecycleListenerMgr.add(listeners);
	}

	void setClientValidator(ClientValidator clientValidator) {
	    this.clientValidator = clientValidator;
    }
	
	private void sendResponseIfNecessary(WsMessage message, ClientInfo clientInfo) {
		if (message.getHeader().isNeedResponse()) {
			ResponseMessage respMsg = new ResponseMessage();
			respMsg.setRespMessageId(message.getHeader().getMessageId());
			respMsg.setSuccess(true);
			this.sendMessage(respMsg, clientInfo.getClientId());
		}
	}
}
