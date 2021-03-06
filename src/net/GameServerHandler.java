/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss <aaron@deviant-core.net>
    				Patrick Huy <patrick.huy@frz.cc>
					Matthias Butz <matze@odinms.de>
					Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net;

import client.GameClient;
import constants.ServerConstants;
import net.server.Server;
import tools.AesCrypto;
import tools.PacketCreator;
import tools.Output;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import tools.GameLogger;

public class GameServerHandler extends IoHandlerAdapter {

	private PacketProcessor processor;
	private byte world = -1, channel = -1;

	public GameServerHandler(PacketProcessor processor) {
		this.processor = processor;
	}

	public GameServerHandler(PacketProcessor processor, byte channel, byte world) {
		this.processor = processor;
		this.channel = channel;
		this.world = world;
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		Runnable r = ((GamePacket) message).getOnSend();
		if (r != null) {
			r.run();
		}
		super.messageSent(session, message);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		/*
		 * synchronized (session) { MapleClient client = ((MapleClient)
		 * session.getAttribute(MapleClient.CLIENT_KEY)); if (client != null) {
		 * client.disconnect(); } }
		 */
		session.close(true);
		GameLogger.print(GameLogger.EXCEPTION_CAUGHT, cause);
		// sessionClosed should be called
	}

	@Override
	public void sessionOpened(IoSession session) {
		if (!Server.getInstance().isOnline()) {
			session.close(true);
			return;
		}
		if (channel > -1 && world > -1) {
			if (Server.getInstance().getChannel(world, channel) == null) {
				session.close(true);
				return;
			}
		} else {
			Output.print("IoSession with " + session.getRemoteAddress() + " opened.");
		}

		byte key[] = {0x13, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, (byte) 0xB4, 0x00, 0x00, 0x00, 0x1B, 0x00, 0x00, 0x00, 0x0F, 0x00, 0x00, 0x00, 0x33, 0x00, 0x00, 0x00, 0x52, 0x00, 0x00, 0x00};
		byte ivRecv[] = {70, 114, 122, 82};
		byte ivSend[] = {82, 48, 120, 115};
		ivRecv[3] = (byte) (Math.random() * 255);
		ivSend[3] = (byte) (Math.random() * 255);
		AesCrypto sendCypher = new AesCrypto(key, ivSend, (short) (0xFFFF - ServerConstants.VERSION));
		AesCrypto recvCypher = new AesCrypto(key, ivRecv, (short) ServerConstants.VERSION);
		GameClient client = new GameClient(sendCypher, recvCypher, session);
		client.setWorldId(world);
		client.setChannelId(channel);
		session.write(PacketCreator.getHello(ServerConstants.VERSION, ivSend, ivRecv));
		session.setAttribute(GameClient.CLIENT_KEY, client);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		synchronized (session) {
			GameClient client = (GameClient) session.getAttribute(GameClient.CLIENT_KEY);
			if (client != null) {
				try {
					client.disconnect();
				} finally {
					session.removeAttribute(GameClient.CLIENT_KEY);
					client.empty();
				}
			}
		}
		super.sessionClosed(session);
	}

	@Override
	public void messageReceived(IoSession session, Object message) {
		byte[] content = (byte[]) message;
		SeekableLittleEndianAccessor reader = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream(content));
		short packetId = reader.readShort();
		GameClient client = (GameClient) session.getAttribute(GameClient.CLIENT_KEY);
		PacketHandler packetHandler = processor.getHandler(packetId);

		if (packetHandler != null && packetHandler.validateState(client)) {
			try {
				packetHandler.handlePacket(reader, client);
			} catch (Throwable t) {
			}
		}
	}

	@Override
	public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
		GameClient client = (GameClient) session.getAttribute(GameClient.CLIENT_KEY);
		if (client != null) {
			client.sendPing();
		}
		super.sessionIdle(session, status);
	}
}
