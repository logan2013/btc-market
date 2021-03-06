package com.fafabtc.app.ui.fragment;

import android.arch.lifecycle.Observer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.View;

import com.fafabtc.app.R;
import com.fafabtc.app.databinding.FragmentMainBinding;
import com.fafabtc.app.ui.base.BaseFragment;
import com.fafabtc.app.ui.base.BindLayout;
import com.fafabtc.app.utils.NetworkUtils;
import com.fafabtc.app.utils.UiUtils;
import com.fafabtc.app.vm.MainViewModel;
import com.fafabtc.data.consts.DataBroadcasts;

/**
 * Created by jastrelax on 2018/1/7.
 */
@BindLayout(R.layout.fragment_main)
public class MainFragment extends BaseFragment<FragmentMainBinding> {

    private static final int[] BOTTOM_NAV_IDS = {R.id.main_bottom_nav_assets,
            R.id.main_bottom_nav_tickers,
            R.id.main_bottom_nav_account};
    private static final SparseIntArray BOTTOM_NAV_POSITION_MAP = new SparseIntArray(){
        {
            for (int pos = 0; pos < BOTTOM_NAV_IDS.length; pos++) {
                put(BOTTOM_NAV_IDS[pos], pos);
            }
        }
    };

    private MainViewModel mainViewModel;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getContext().registerReceiver(initiateReceiver, new IntentFilter(){
            {
                addAction(DataBroadcasts.Actions.ACTION_INITIATE_EXCHANGE);
                addAction(DataBroadcasts.Actions.ACTION_INITIATE_ASSETS);
                addAction(DataBroadcasts.Actions.ACTION_DATA_INITIALIZED);
            }
        });

        getContext().registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        mainViewModel = getViewModel(MainViewModel.class);
        mainViewModel.isDataInitialized().observe(this, initiateObserver);
        mainViewModel.queryIsDataInitialized();

        binding.pagerMain.setOffscreenPageLimit(2);
        binding.pagerMain.setAdapter(new MainFragmentPagerAdapter(getChildFragmentManager()));
        binding.pagerMain.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position >= 0 && position < BOTTOM_NAV_IDS.length)
                    binding.bottomNavMain.setSelectedItemId(BOTTOM_NAV_IDS[position]);
            }
        });

        binding.bottomNavMain.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                binding.pagerMain.setCurrentItem(BOTTOM_NAV_POSITION_MAP.get(id));
                return true;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getContext().unregisterReceiver(initiateReceiver);
        getContext().unregisterReceiver(networkStateReceiver);
    }

    private Observer<Boolean> initiateObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable Boolean aBoolean) {
            if (aBoolean != null && aBoolean) {
                hideStateTip();
            } else {
                showStateTip();
            }
            checkNetworkState();
        }
    };

    private BroadcastReceiver initiateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                String exchange = intent.getStringExtra(DataBroadcasts.Extras.EXTRA_EXCHANGE_NAME);
                switch (intent.getAction()) {
                    case DataBroadcasts.Actions.ACTION_INITIATE_EXCHANGE:
                        binding.tvStateTip.setText(String.format("初始化%s交易所数据...", exchange));
                        break;
                    case DataBroadcasts.Actions.ACTION_INITIATE_ASSETS:
                        binding.tvStateTip.setText(String.format("初始化%s资产...", exchange));
                        break;
                    case DataBroadcasts.Actions.ACTION_FETCH_TICKERS:
                        binding.tvStateTip.setText(String.format("获取%s最新行情...", exchange));
                        break;
                    case DataBroadcasts.Actions.ACTION_DATA_INITIALIZED:
                        hideStateTip();
                        break;
                }
            }
        }
    };

    private void checkNetworkState() {
        if (!NetworkUtils.isConnected(getContext())) {
            showNetworkState();
        }
    }

    private void showStateTip() {
        binding.tvStateTip.setPadding(0, UiUtils.getStatusBarHeight(getContext()), 0, getResources().getDimensionPixelSize(R.dimen.tv_padding_small));
        binding.tvStateTip.setVisibility(View.VISIBLE);
    }

    private void hideStateTip() {
        binding.tvStateTip.setVisibility(View.GONE);
    }

    private void showNetworkState() {
        binding.tvAppTip.setPadding(0, UiUtils.getStatusBarHeight(getContext()), 0, getResources().getDimensionPixelSize(R.dimen.tv_padding_small));
        binding.tvAppTip.setBackgroundColor(getContext().getResources().getColor(R.color.deep_orange));
        binding.tvAppTip.setText("网络未连接");
        binding.tvAppTip.setVisibility(View.VISIBLE);
    }

    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (NetworkUtils.isConnected(context)) {
                    binding.tvAppTip.setVisibility(View.GONE);
                } else {
                    showNetworkState();
                }
            }
        }
    };

    private class MainFragmentPagerAdapter extends FragmentPagerAdapter {

        private int[] titles = {R.string.assets, R.string.tickers, R.string.mine};

        public MainFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return AssetsFragment.newInstance();
                case 1:
                    return TickerPagerFragment.newInstance();
                case 2:
                    return AccountFragment.newInstance();
                default:
                    return ErrorFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getString(titles[position]);
        }
    }
}
