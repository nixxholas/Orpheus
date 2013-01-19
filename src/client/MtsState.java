package client;

public class MtsState {
	private String search = null;
	private int ci = 0; // WTF IS THIS?
	private int currentPage, currentType = 0, currentTab = 1;

	public String getSearch() {
		return search;
	}
	
	public int getCurrentCI() {
		return ci;
	}

	public int getCurrentPage() {
		return currentPage;
	}
	
	public int getCurrentTab() {
		return currentTab;
	}

	public int getCurrentType() {
		return currentType;
	}

	public void setSearch(String find) {
		search = find;
	}

	public void changeCI(int type) {
		this.ci = type;
	}

	public void changePage(int page) {
		this.currentPage = page;
	}

	public void changeTab(int tab) {
		this.currentTab = tab;
	}

	public void changeType(int type) {
		this.currentType = type;
	}
}
