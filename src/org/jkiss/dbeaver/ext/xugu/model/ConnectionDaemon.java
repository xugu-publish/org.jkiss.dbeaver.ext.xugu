package org.jkiss.dbeaver.ext.xugu.model;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class ConnectionDaemon implements Runnable {
	private Connection conn;
	private DBPConnectionConfiguration conf;
	private Properties props;
	private Driver driver;
	private String url;
	private int sleepTime;
	private int dbTime;
	protected static final Log log = Log.getLog(ConnectionDaemon.class);
	
	public ConnectionDaemon(Connection conn, DBPConnectionConfiguration conf,  Properties props, Driver driver, String url, int dbTime) {
		this.conn = conn;
		this.conf = conf;
		this.props = props;
		this.driver = driver;
		this.url = url;
		this.dbTime = dbTime;
		this.sleepTime = checkSleepTime(XuguConstants.DEFAULT_SLEEP_TIME);
	}
	
	public int checkSleepTime(int sleepTime) {
		if(this.dbTime!=0) {
			//若默认间隔时间大于连接最大空闲时间则将间隔时间设为空闲时间的1/3
			if(sleepTime > this.dbTime) {
				sleepTime = this.dbTime/3;
			}
			//否则直接返回设置的sleepTime
		}
		return sleepTime;
	}
	
	public void setSleepTime(int time) {
		this.sleepTime = checkSleepTime(time);
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Statement stmt = conn.createStatement();
				stmt.executeQuery("select 1 from dual");
				Thread.sleep(sleepTime);
				System.out.println("Daemon task running "+sleepTime+" "+dbTime);
				log.warn("Xugu connect daemon running: Do select 1!");
			} catch (InterruptedException | SQLException e) {
				e.printStackTrace();
				System.out.println("Connect time out! Do reconnect!");
				log.warn("Xugu connect link down! Do reconnect!");
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
