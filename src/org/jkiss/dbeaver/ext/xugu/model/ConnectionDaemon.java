package org.jkiss.dbeaver.ext.xugu.model;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class ConnectionDaemon implements Runnable {
	private Connection conn;
	private DBPConnectionConfiguration conf;
	private Properties props;
	private Driver driver;
	private String url;
	private int sleepTime;
	
	public ConnectionDaemon(Connection conn, DBPConnectionConfiguration conf,  Properties props, Driver driver, String url) {
		this.conn = conn;
		this.conf = conf;
		this.props = props;
		this.driver = driver;
		this.url = url;
		this.sleepTime = XuguConstants.DEFAULT_SLEEP_TIME;
	}
	
	public void setSleepTime(int time) {
		this.sleepTime = time;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Statement stmt = conn.createStatement();
				stmt.executeQuery("select 1 from dual");
				Thread.sleep(sleepTime);
				System.out.println("Daemon task running");
			} catch (InterruptedException | SQLException e) {
				e.printStackTrace();
				System.out.println("Connect time out! Do reconnect!");
				try {
					if (driver == null) {
	                    this.conn = DriverManager.getConnection(url, props);
	                } else {
							this.conn = driver.connect(url, props);
	                }
					System.out.println("Reconnect successed!");
				} catch (SQLException e1) {
					System.out.println("Reconnect failed!");
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}

}
