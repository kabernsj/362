package model;

import interfaces.RestaurantInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

public class Restaurant implements RestaurantInterface {
	
	private StorageSupport storageSupport = null;
	private RestaurantStatistics restaurantStatistics = null;
	private int orderID = 0;
	private boolean authenticated = false;
	
	public Restaurant() {
		restaurantStatistics = new RestaurantStatistics();
		//passes restaurantStatistics for initial populate from file
		storageSupport = new StorageSupport(restaurantStatistics);
		orderID = restaurantStatistics.getOrderID();
	}
	
	@Override
	public boolean editTableCount(int newTableCount) {
		if (!authenticated) {
			return false;
		}
		if (newTableCount < 0) {
			return false;
		}
		int oldTableCount = restaurantStatistics.getTableCount();
		if (oldTableCount == -1) {
			for (int i = 1; i <= newTableCount; i++) {
				if(!storageSupport.putTable(new Table(i))) {
					return false;
				}
			}
		}
		else if (oldTableCount != newTableCount && restaurantStatistics.updateTableCount(newTableCount)) {
			if (newTableCount > oldTableCount) {
				//add extra tables
				while (newTableCount != oldTableCount) {
					if(!storageSupport.putTable(new Table(oldTableCount+1))) {
						return false;
					}
					oldTableCount++;
				}
			} else {
				while (newTableCount != oldTableCount) {
					if(!storageSupport.deleteTable(oldTableCount)) {
						return false;
					}
					oldTableCount--;
				}
			}
		}
		restaurantStatistics.updateTableCount(newTableCount);
		return true;
	}

	@Override
	public boolean addServer(String serverID) {
		if (!authenticated) {
			return false;
		}
		Server s = new Server(serverID);
		return storageSupport.putServer(s);
	}

	@Override
	public boolean deleteServer(String serverID) {
		if (!authenticated) {
			return false;
		}
		return storageSupport.deleteServer(serverID);
	}

	@Override
	public String getTableInfo(int tableNumber) {
		Table t = storageSupport.getTable(tableNumber);
		if(t == null)
		{
			return null;
		}
		return t.getTableInfo();
	}

	@Override
	public boolean changeTableServer(int tableNumber, String newServerID) {
		Table table = storageSupport.getTable(tableNumber);
		if (table == null) {
			return false;
		}
		Server newServer = storageSupport.getServer(newServerID);
		if (newServer == null) {
			return false;
		}
		Server server = table.getServer();
		server.decrementTableCount();
		table.setServer(newServer);
		newServer.incrementTableCount();
		return true;
	}

	@Override
	public boolean setTableToInUse(int tableNumber, String serverID) {
		Table table = storageSupport.getTable(tableNumber);
		if (table == null) {
			return false;
		}
		Server server = storageSupport.getServer(serverID);
		return table.setToInUse(server);
	}

	@Override
	public boolean setTableToReady(int tableNumber) {
		Table table = storageSupport.getTable(tableNumber);
		if (table == null) {
			return false;
		}
		table.setToReady();
		return true;
	}

	@Override
	public String getServerTables(String serverID) {
		String ret = "Server " + serverID + " tables: ";
		ArrayList<Table> allTables = storageSupport.getAllTables();
		for(Table t : allTables)
		{
			Server s = t.getServer();
			if(s == null);
			else if(s.getServerID().equals(serverID))
			{
				ret += t.getTableNumber() + " ";
			}
		}
		return ret;
	}

	@Override
	public boolean submitFeedback(String serverID, String feedback, boolean good) {
		Server s = storageSupport.getServer(serverID);
		return s.submitFeedback(feedback, good);
	}

	@Override
	public String getServerFeedback(String serverID) {
		if (authenticated) {
			return null;
		}
		Server s = storageSupport.getServer(serverID);
		if(s==null)
			return null;
		return s.getFeedback();
	}
	
	@Override
	public boolean createOrder(int tableNumber, int drink, int appetizer, int meal, int side, String special) {
		Table t = storageSupport.getTable(tableNumber);
		if (t == null)
			return false;
		restaurantStatistics.updateOrderID(++orderID);
		restaurantStatistics.updateDrinkCount(drink, true);
		restaurantStatistics.updateMealCount(meal, true);
		restaurantStatistics.updateAppetizerCount(appetizer, true);
		restaurantStatistics.updateSideCount(side, true);
		Order o = new Order(orderID, drink, appetizer, meal, side, special);
		return t.putOrder(o);
	}

	@Override
	public boolean authenticate(String passcode) {
		authenticated = storageSupport.authenticatePasscode(passcode);
		return authenticated;
	}


	public void dumpToFile() {
		storageSupport.dumpToFile(restaurantStatistics);
	}


	@Override
	public boolean modifyOrder(int tableNumber, int orderID, String field,
			String newvalue) {
		Table t =  storageSupport.getTable(tableNumber);
		if (t == null)
			return false;
		Order o = t.getOrder(orderID);
		if (o == null)
			return false;
		if(o.modifyOrder(field, newvalue, restaurantStatistics) == false)
			return false;
		return t.putOrder(o);
	}
	
	@Override
	public boolean deleteOrder(int tableNumber, int orderID)
	{
		Table t = storageSupport.getTable(tableNumber);
		if (t == null)
			return false;
		return t.deleteOrder(orderID);
	}
	
	@Override
	public ArrayList<Order> obtainOrderListByCreation()
	{
		ArrayList<Order> ret = new ArrayList<Order>();
		ArrayList<Table> tables = storageSupport.getAllTables();
		boolean added;
		for (Table t : tables)
		{
			HashMap<Integer, Order> orders = t.getAllOrders();
			for (Entry<Integer, Order> entry : orders.entrySet())
			{
				added = false;
				Order o = entry.getValue();
				if(o.getStatus().equals("Ordered"))
				{
					for (int i = 0; i<ret.size(); i++)
					{
						if (ret.get(i).getTimestamp() >= o.getTimestamp())
						{
							ret.add(i,o);
							added = true;
							break;
						}
					}
					if(!added)
						ret.add(o);
				}
			}
		}
		return ret;
	}


	@Override
	public String getServersAndNumberOfTables() {
		Collection<Server> servers = storageSupport.getServers();
		String returnString = "";
		for(Server server : servers) {
			returnString += "Server ID: " + server.getServerID() + "\nServicing: " + server.getTableCount() + " tables.\n\n";
		}
		return returnString;
	}


	@Override
	public String getTablesChecks(int tableNumber) {
		Table t = storageSupport.getTable(tableNumber);
		ArrayList<Check> checks = t.getAllChecks();
		String returnString = "";
		int count = 1;
		for (Check check : checks) {
			returnString += "Check " + count + ":\n" + check.toString() + "\n";
			count++;
		}
		return returnString;
	}


	@Override
	public String getTablesOrders(int tableNumber) {
		Table t = storageSupport.getTable(tableNumber);
		Collection<Order> orders = t.getAllOrders().values();
		String returnString = "";
		for (Order order : orders) {
			returnString += "Order: " + order.toString() + "\n";
		}
		return returnString;
	}

	@Override
	public boolean generateChecks(int tableNumber, ArrayList<String> orders) {
		Table t = storageSupport.getTable(tableNumber);
		if(t==null)
			return false;
		for(String oList : orders)
		{
			if(t.addCheck(oList) == false)
				return false;
		}
		return true;
	}

	public int checkItemPopularity(int type, int itemIndex) {
		if (type == 1)
		{
			return restaurantStatistics.getDrinkCount(itemIndex);
		}
		else if (type == 2)
		{
			return restaurantStatistics.getMealCount(itemIndex);
		}
		else if (type == 3)
		{
			return restaurantStatistics.getAppetizerCount(itemIndex);
		}
		else if (type == 4)
		{
			return restaurantStatistics.getSideCount(itemIndex);
		}
		return -1;
	}
}
