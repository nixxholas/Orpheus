/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss

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
package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.security.NoSuchAlgorithmException;
import tools.DatabaseConnection;
import tools.HashCreator;
import tools.GameLogger;

public class AutoRegister {
	private static final int ACCOUNTS_PER_IP = 4;

	public static boolean getAccountExists(String login) {
		boolean accountExists = false;
		Connection con = DatabaseConnection.getConnection();
		try (PreparedStatement ps = getSelectAccountByName(con, login);
				ResultSet rs = ps.executeQuery();) {
			
			if (rs.first()) {
				accountExists = true;
			}
			
		} catch (Exception e) {
		}
		return accountExists;
	}

	private static PreparedStatement getSelectAccountByName(Connection connection, String login) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT `name` FROM `accounts` WHERE `name` = ?");
		ps.setString(1, login);
		return ps;
	}

	public static boolean createAccount(String login, String pwd, String endpoint) {
		Connection con;
		try {
			con = DatabaseConnection.getConnection();
		} catch (Exception e) {
			GameLogger.print(GameLogger.EXCEPTION_CAUGHT, "There's a problem with automatic registration.\r\n" + e);
			return false;
		}
		
		final String ip = endpoint.substring(1, endpoint.lastIndexOf(':'));
		try (PreparedStatement ipc = getSelectAccountByIp(con, ip);
				ResultSet rs = ipc.executeQuery();) {
			
			if (rs.last() == true && rs.getRow() > ACCOUNTS_PER_IP) {
				return false;
			}
			
		} catch (SQLException e) {
			GameLogger.print(GameLogger.EXCEPTION_CAUGHT, "There's a problem with automatic registration.\r\n" + e);
			return false;
		}		

		try (PreparedStatement ps = getInsertAccount(con, login, pwd, ip);) {
			
			ps.executeUpdate();
			return true;
			
		} catch (NoSuchAlgorithmException e) {
			GameLogger.print(GameLogger.EXCEPTION_CAUGHT, "There's a problem with automatic registration.\r\n" + e);
			return false;
		} catch (SQLException ex) {
			GameLogger.print(GameLogger.EXCEPTION_CAUGHT, "There's a problem with automatic registration.\r\n" + ex);
			return false;
		}
	}

	private static PreparedStatement getInsertAccount(Connection connection, String login, String pwd, final String ip) 
			throws SQLException, NoSuchAlgorithmException {
		
		PreparedStatement ps = connection.prepareStatement("INSERT INTO `accounts` (`name`, `password`, `email`, `birthday`, `macs`, `lastknownip`) VALUES (?, ?, ?, ?, ?, ?)");
		
		ps.setString(1, login);
		ps.setString(2, HashCreator.getHash(pwd));
		ps.setString(3, "no@email.provided");
		ps.setString(4, "0000-00-00");
		ps.setString(5, "00-00-00-00-00-00");
		ps.setString(6, ip);
		
		return ps;
	}

	private static PreparedStatement getSelectAccountByIp(Connection connection, final String ip) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT `lastknownip` FROM `accounts` WHERE `lastknownip` = ?");
		ps.setString(1, ip);
		return ps;
	}
}