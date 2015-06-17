package william.example.pulltorefreshlistsimple;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnLastItemVisibleListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.State;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.handmark.pulltorefresh.library.extras.SoundPullEventListener;

public class MainActivity extends ActionBarActivity {

	private LinkedList<String> mListItems;
	private PullToRefreshListView mPullToRefreshListView;
	private ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
		initData();
		initListView();
	}

	private void initListView() {

		// 设置一个监听器被调用时应该刷新列表
		mPullToRefreshListView
				.setOnRefreshListener(new OnRefreshListener<ListView>() {

					@Override
					public void onRefresh(
							PullToRefreshBase<ListView> refreshView) {
						String label = DateUtils.formatDateTime(
								getApplicationContext(),
								System.currentTimeMillis(),
								DateUtils.FORMAT_SHOW_TIME
										| DateUtils.FORMAT_SHOW_DATE
										| DateUtils.FORMAT_ABBREV_ALL);
						// 更新最后更新标签
						refreshView.getLoadingLayoutProxy()
								.setLastUpdatedLabel(label);
						if (refreshView.getCurrentMode() == Mode.PULL_FROM_END) {
							// 在这里做刷新的工作(加载更多)
							new GetDataTask(false).execute();
						} else {
							// 在这里做刷新的工作(下拉刷新)
							new GetDataTask(true).execute();
						}
					}

				});

		// 添加列表底部监听器
		mPullToRefreshListView
				.setOnLastItemVisibleListener(new OnLastItemVisibleListener() {

					@Override
					public void onLastItemVisible() {
						Toast.makeText(MainActivity.this, "End of List!",
								Toast.LENGTH_SHORT).show();
					}
				});
		// 添加声音事件监听
		SoundPullEventListener<ListView> soundListener = new SoundPullEventListener<ListView>(
				this);
		soundListener.addSoundEvent(State.PULL_TO_REFRESH, R.raw.pull_event); // 下拉刷新声音事件
		soundListener.addSoundEvent(State.REFRESHING, R.raw.refreshing_sound); // 刷新声音事件
		soundListener.addSoundEvent(State.RESET, R.raw.reset_sound); // 重置声音事件，刷新完成之后
		// State.MANUAL_REFRESHING 手动刷新状态
		mPullToRefreshListView.setOnPullEventListener(soundListener);

		ListView actualListView = mPullToRefreshListView.getRefreshableView();
		// 当为系统菜单注册时需要使用实际的ListView
		registerForContextMenu(actualListView);
	}

	private void initData() {
		mListItems = new LinkedList<String>();
		mListItems.addAll(Arrays.asList(mStrings));
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mListItems);
		// 你也可以只使用setListAdapter(mAdapter)，或者setAdapter();都可以；
		mPullToRefreshListView.setAdapter(adapter);
	}

	private class GetDataTask extends AsyncTask<Void, Void, String[]> {

		private boolean mode;

		/**
		 * 指明标识刷新还是加载
		 * 
		 * @param mode
		 *            如果是true表示刷新，否则是加载标识
		 */
		public GetDataTask(boolean mode) {
			this.mode = mode;
		}

		@Override
		protected String[] doInBackground(Void... params) {
			// 模拟后台工作
			// 在这里你可以从网络、数据库获取新的数据
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				// This good..
			}
			return mStrings;
		}

		@Override
		protected void onPostExecute(String[] result) {
			// result 就是获取新的数据
			if (mode)
				mListItems.addFirst("Added after refresh...So Great!!!!!!!!!");
			else
				mListItems.add("Added after refresh...So Great!!!!!!!!!");
			adapter.notifyDataSetChanged();
			// 当刷新完成之后调用这个方法 当前刷新标记为完成，会重置UI并且隐藏Refreshing View.
			mPullToRefreshListView.onRefreshComplete();
			super.onPostExecute(result);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_MANUAL_REFRESH, 0, "手动刷新");
		menu.add(0, MENU_DISABLE_SCROLL, 1, mPullToRefreshListView
				.isScrollingWhileRefreshingEnabled() ? "关闭刷新可以滚动" : "开启刷新可以滚动");
		menu.add(
				0,
				MENU_SET_MODE,
				2,
				mPullToRefreshListView.getMode() == Mode.BOTH ? "更改为 MODE_PULL_DOWN"
						: "更改为 MODE_PULL_BOTH");
		menu.add(0, MENU_DEMO, 3, "演示");
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem disableItem = menu.findItem(MENU_DISABLE_SCROLL);
		disableItem.setTitle(mPullToRefreshListView
				.isScrollingWhileRefreshingEnabled() ? "关闭刷新可以滚动" : "开启刷新可以滚动");
		MenuItem setModeItem = menu.findItem(MENU_SET_MODE);
		setModeItem
				.setTitle(mPullToRefreshListView.getMode() == Mode.BOTH ? "更改为 MODE_PULL_DOWN"
						: "更改为 MODE_PULL_BOTH");
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_MANUAL_REFRESH:
			// 手动刷新。
			new GetDataTask(true).execute();
			mPullToRefreshListView.setRefreshing(false);
			break;
		case MENU_DISABLE_SCROLL:
			mPullToRefreshListView
					.setScrollingWhileRefreshingEnabled(!mPullToRefreshListView
							.isScrollingWhileRefreshingEnabled());
			break;
		case MENU_SET_MODE:
			mPullToRefreshListView
					.setMode(mPullToRefreshListView.getMode() == Mode.BOTH ? Mode.PULL_FROM_START
							: Mode.BOTH);
			break;
		case MENU_DEMO:
			mPullToRefreshListView.demo();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo adapterMenu = (AdapterContextMenuInfo) menuInfo;
		menu.setHeaderTitle("Item: "
				+ mPullToRefreshListView.getRefreshableView()
						.getItemAtPosition(adapterMenu.position));
		menu.add("Item1");
		menu.add("Item2");
		menu.add("Item3");
		menu.add("Item4");
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	private String[] mStrings = { "Abbaye de Belloc",
			"Abbaye du Mont des Cats", "Abertam", "Abondance", "Ackawi",
			"Acorn", "Adelost", "Affidelice au Chablis", "Afuega'l Pitu",
			"Airag", "Airedale", "Aisy Cendre", "Allgauer Emmentaler",
			"Abbaye de Belloc", "Abbaye du Mont des Cats", "Abertam",
			"Abondance", "Ackawi", "Acorn", "Adelost", "Affidelice au Chablis",
			"Afuega'l Pitu", "Airag", "Airedale", "Aisy Cendre",
			"Allgauer Emmentaler" };

	static final int MENU_MANUAL_REFRESH = 0;
	static final int MENU_DISABLE_SCROLL = 1;
	static final int MENU_SET_MODE = 2;
	static final int MENU_DEMO = 3;
}
