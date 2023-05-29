/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import input.EventQueue;
import input.ExternalEvent;
import input.ScheduledUpdatesQueue;

import java.util.*;

/**
 * World contains all the nodes and is responsible for updating their
 * location and connections.
 */
import java.util.Arrays;



class AccessPoint
{
	public Coord location;

	public AccessPoint() {
	}

	public float Strength;

	public AccessPoint(Coord station, float streng) {
		this.location = station;
		Strength = streng;
	}
}
public class World {
	private static ArrayList<AccessPoint> AP_list= new ArrayList<AccessPoint>();
	private static final int N = 4;
	private static final int M = 2;

	public static float [][] SPEED = new float[N][N];
	private static final float INF = Float.POSITIVE_INFINITY;
	private float[] labelX, labelY;  // 二分图的节点标号
	private int[] matchX, matchY;  // 记录二分图中Y部分节点匹配的X部分节点

	public float maxWeightMatching() {
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				labelX[i] = Math.max(labelX[i], SPEED[i][j]);
			}
		}
		for (int i = 0; i < N; i++) {
			while (true) {
				boolean[] visX = new boolean[N], visY = new boolean[N];
				if (dfs(i, visX, visY)) break;
				float delta = INF;
				for (int j = 0; j < N; j++) {
					if (visX[j]) {
						for (int k = 0; k < N; k++) {
							if (!visY[k]) {
								delta = Math.min(delta, labelX[j] + labelY[k] - SPEED[j][k]);
							}
						}
					}
				}
				for (int j = 0; j < N; j++) {
					if (visX[j]) {
						labelX[j] -= delta;
					}
				}
				for (int j = 0; j < N; j++) {
					if (visY[j]) {
						labelY[j] += delta;
					}
				}
			}
		}
		float weight = 0;
		for (int i = 0; i < N; i++) {
			if (matchX[i] != -1) {
				weight += SPEED[i][matchX[i]];
			}
		}
		return weight;
	}

	private boolean dfs(int x, boolean[] visX, boolean[] visY) {
		visX[x] = true;
		for (int y = 0; y < N; y++) {
			if (!visY[y] && Math.abs(labelX[x] + labelY[y] - SPEED[x][y]) < 1e-6) {
				visY[y] = true;
				if (matchY[y] == -1 || dfs(matchY[y], visX, visY)) {
					matchX[x] = y;
					matchY[y] = x;
					return true;
				}
			}
		}
		return false;
	}

	/** name space of optimization settings ({@value})*/
	public static final String OPTIMIZATION_SETTINGS_NS = "Optimization";

	/**
	 * Should the order of node updates be different (random) within every 
	 * update step -setting id ({@value}). Boolean (true/false) variable. 
	 * Default is @link {@link #DEF_RANDOMIZE_UPDATES}.
	 */
	public static final String RANDOMIZE_UPDATES_S = "randomizeUpdateOrder";
	/** should the update order of nodes be randomized -setting's default value
	 * ({@value}) */
	public static final boolean DEF_RANDOMIZE_UPDATES = true;
	
	/**
	 * Should the connectivity simulation be stopped after one round 
	 * -setting id ({@value}). Boolean (true/false) variable. 
	 */
	public static final String SIMULATE_CON_ONCE_S = "simulateConnectionsOnce";

	private int sizeX;
	private int sizeY;
	private List<EventQueue> eventQueues;
	private double updateInterval;
	private SimClock simClock;
	private double nextQueueEventTime;
	private EventQueue nextEventQueue;
	/** list of nodes; nodes are indexed by their network address */
	private List<DTNHost> hosts;
	private boolean simulateConnections;
	/** nodes in the order they should be updated (if the order should be 
	 * randomized; null value means that the order should not be randomized) */
	private ArrayList<DTNHost> updateOrder;
	/** is cancellation of simulation requested from UI */
	private boolean isCancelled;
	private List<UpdateListener> updateListeners;
	/** Queue of scheduled update requests */
	private ScheduledUpdatesQueue scheduledUpdates;
	private boolean simulateConOnce;
	private boolean isConSimulated;

	/**广播总线*/
	public static ArrayList<Message>  BROADCAST_MESSAGE = new ArrayList<>();
	public static int BROADCAST_BUS = 0;
	public static int BROADCAST_BUS_DELAY = 0;
	public static int DELAY_SET = 3 ;
	public static int LAST_BROADCAST_BUS = 0;
	public static int LISTEN_BUS = 0;



	/**
	 * Constructor.
	 */

	void init()
	{
		for(int i = 0; i < N; i++) {
			for(int j = 0; j < N; j++) {
				SPEED[i][j] =INF;
			}
		}
		labelX = new float[N];
		labelY = new float[N];
		Arrays.fill(labelX, -INF);
		Arrays.fill(labelY, 0);
		matchX = new int[N];
		matchY = new int[N];
		Arrays.fill(matchX, -1);
		Arrays.fill(matchY, -1);
	}

	public World(List<DTNHost> hosts, int sizeX, int sizeY, 
			double updateInterval, List<UpdateListener> updateListeners,
			boolean simulateConnections, List<EventQueue> eventQueues) {
		this.hosts = hosts;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.updateInterval = updateInterval;
		this.updateListeners = updateListeners;
		this.simulateConnections = simulateConnections;
		this.eventQueues = eventQueues;
		
		this.simClock = SimClock.getInstance();
		this.scheduledUpdates = new ScheduledUpdatesQueue();
		this.isCancelled = false;
		this.isConSimulated = false;


		Coord AP_1_STATION = new Coord(600,600);
		Coord AP_2_STATION = new Coord(600,800);
		AP_list.add(new AccessPoint(AP_1_STATION,500));
		AP_list.add(new AccessPoint(AP_2_STATION,600));

		init();


		setNextEventQueue();
		initSettings();
	}

	/**
	 * Initializes settings fields that can be configured using Settings class
	 */
	private void initSettings() {
		Settings s = new Settings(OPTIMIZATION_SETTINGS_NS);
		boolean randomizeUpdates = DEF_RANDOMIZE_UPDATES;

		if (s.contains(RANDOMIZE_UPDATES_S)) {
			randomizeUpdates = s.getBoolean(RANDOMIZE_UPDATES_S);
		}
		simulateConOnce = s.getBoolean(SIMULATE_CON_ONCE_S, false);
		
		if(randomizeUpdates) {
			// creates the update order array that can be shuffled
			this.updateOrder = new ArrayList<DTNHost>(this.hosts);
		}
		else { // null pointer means "don't randomize"
			this.updateOrder = null;
		}
	}

	/**
	 * Moves hosts in the world for the time given time initialize host 
	 * positions properly. SimClock must be set to <CODE>-time</CODE> before
	 * calling this method.
	 * @param time The total time (seconds) to move
	 */
	public void warmupMovementModel(double time) {
		if (time <= 0) {
			return;
		}

		while(SimClock.getTime() < -updateInterval) {
			moveHosts(updateInterval);
			simClock.advance(updateInterval);
		}

		double finalStep = -SimClock.getTime();

		moveHosts(finalStep);
		simClock.setTime(0);	
	}

	/**
	 * Goes through all event Queues and sets the 
	 * event queue that has the next event.
	 */
	public void setNextEventQueue() {
		EventQueue nextQueue = scheduledUpdates;
		double earliest = nextQueue.nextEventsTime();

		/* find the queue that has the next event */
		for (EventQueue eq : eventQueues) {
			if (eq.nextEventsTime() < earliest){
				nextQueue = eq;	
				earliest = eq.nextEventsTime();
			}
		}

		this.nextEventQueue = nextQueue;
		this.nextQueueEventTime = earliest;
	}

	/** 
	 * Update (move, connect, disconnect etc.) all hosts in the world.
	 * Runs all external events that are due between the time when
	 * this method is called and after one update interval.
	 */
	public float getDistance(Coord a,Coord b)
	{
		return (float) Math.sqrt(Math.pow(a.getX()-b.getX(),2)+Math.pow(a.getY()-b.getY(),2));
	}
	public void update ()
	{

		double runUntil = SimClock.getTime() + this.updateInterval;
		System.out.println("=====================世界更新====================");
		System.out.println("当前时间："+runUntil);

		int index =0;
		for (DTNHost host: hosts)
		{
			if (!host.getName().startsWith("AP")) {
				System.out.println("-----------------------");
				System.out.println("当前节点："+ host.toString()+"，坐标为："+host.getLocation());
				for (int j=0;j<N;j++) {
					AccessPoint ap = AP_list.get(j/M);
					float strength = ap.Strength / getDistance(host.getLocation(),ap.location);
					float speed = (float) (Math.log(1+strength)/Math.log(2));
					SPEED[index][j] = speed;
				}
				index++;
			}
		}

		float weight = maxWeightMatching();
		System.out.println("最大下行速率和为：" + weight);
		System.out.println("连接方案为：");
		for (int i = 0; i < N; i++) {
			if (matchX[i] != -1) {
				int y = matchX[i]/M;
				System.out.println("N" + i + " -> AP" + y);
			}
		}

		init();

		while (this.nextQueueEventTime <= runUntil) {
			simClock.setTime(this.nextQueueEventTime);
			ExternalEvent ee = this.nextEventQueue.nextEvent();
			ee.processEvent(this);
			updateHosts(); // update all hosts after every event
			setNextEventQueue();
		}


		moveHosts(this.updateInterval);
		simClock.setTime(runUntil);

		updateHosts();

		/* inform all update listeners */
		for (UpdateListener ul : this.updateListeners) {
			ul.updated(this.hosts);
		}


	}

	/**
	 * Updates all hosts (calls update for every one of them). If update
	 * order randomizing is on (updateOrder array is defined), the calls
	 * are made in random order.
	 */
	private void updateHosts() {
		if (this.updateOrder == null) { // randomizing is off
			for (int i=0, n = hosts.size();i < n; i++) {
				if (this.isCancelled) {
					break;
				}
				hosts.get(i).update(simulateConnections);
			}
		}
		else { // update order randomizing is on
			assert this.updateOrder.size() == this.hosts.size() : 
				"Nrof hosts has changed unexpectedly";
			Random rng = new Random(SimClock.getIntTime());
			Collections.shuffle(this.updateOrder, rng); 
			for (int i=0, n = hosts.size();i < n; i++) {
				if (this.isCancelled) {
					break;
				}
				this.updateOrder.get(i).update(simulateConnections);
			}			
		}
		
		if (simulateConOnce && simulateConnections) {
			simulateConnections = false;
		}
	}

	/**
	 * Moves all hosts in the world for a given amount of time
	 * @param timeIncrement The time how long all nodes should move
	 */
	private void moveHosts(double timeIncrement) {
		for (int i=0,n = hosts.size(); i<n; i++) {
			DTNHost host = hosts.get(i);
			host.move(timeIncrement);			
		}		
	}

	/**
	 * Asynchronously cancels the currently running simulation
	 */
	public void cancelSim() {
		this.isCancelled = true;
	}

	/**
	 * Returns the hosts in a list
	 * @return the hosts in a list
	 */
	public List<DTNHost> getHosts() {
		return this.hosts;
	}

	/**
	 * Returns the x-size (width) of the world 
	 * @return the x-size (width) of the world 
	 */
	public int getSizeX() {
		return this.sizeX;
	}

	/**
	 * Returns the y-size (height) of the world 
	 * @return the y-size (height) of the world 
	 */
	public int getSizeY() {
		return this.sizeY;
	}

	public double getUpdateInterval() {
		return updateInterval;
	}

	/**
	 * Returns a node from the world by its address
	 * @param address The address of the node
	 * @return The requested node or null if it wasn't found
	 */
	public DTNHost getNodeByAddress(int address) {
		if (address < 0 || address >= hosts.size()) {
			throw new SimError("No host for address " + address + ". Address " +
					"range of 0-" + (hosts.size()-1) + " is valid");
		}

		DTNHost node = this.hosts.get(address);
		assert node.getAddress() == address : "Node indexing failed. " + 
			"Node " + node + " in index " + address;

		return node; 
	}

	/**
	 * Schedules an update request to all nodes to happen at the specified 
	 * simulation time.
	 * @param simTime The time of the update
	 */
	public void scheduleUpdate(double simTime) {
		scheduledUpdates.addUpdate(simTime);
	}
}
