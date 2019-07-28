package com.freshworks.https.proxy.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.freshworks.https.proxy.exception.MaxConnectionExceededException;

/**
 * @author Balasubramanian.ramar
 *
 */
public class ClientPool {
	
	private Integer maxConnection;
	public ClientPool (Integer maxConnection) {
		this.maxConnection = maxConnection;
	}
	private Map<Integer, ConcurrentHashMap<String, Integer>> clientPool = new ConcurrentHashMap<Integer,ConcurrentHashMap<String,Integer>>();

	public void checkAndUpdate(String clientID, Integer timestamp) throws MaxConnectionExceededException {
		
		if(clientPool.containsKey(timestamp)) {
			ConcurrentHashMap<String, Integer> childmap = clientPool.get(timestamp);
			if(childmap == null) {
				ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<String, Integer>();
				map.put(clientID, 0);
			}else {
				if(childmap.contains(clientID)) {
					Integer connectionCount = childmap.get(childmap);
					if(connectionCount == maxConnection) {
						// to - Do add error code and msg
						throw new MaxConnectionExceededException();
					}else {
						childmap.put(clientID, connectionCount.intValue()+1);
					}
				}
			}
		}else {
			if(!clientPool.isEmpty()){
				clientPool.clear();
			}
			ConcurrentHashMap<String, Integer> newMap = new ConcurrentHashMap<String, Integer>();
			newMap.put(clientID, 0);
			clientPool.put(timestamp, newMap);
		}
	}
	
	//TO-DO - Anonymous thread to clear the cache
	//To-Do watcher is not required at the moment
	Runnable watcher = () -> {
		
		while(true) {
			try {
				Thread.sleep(1000);
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	};
	
	
	
}
