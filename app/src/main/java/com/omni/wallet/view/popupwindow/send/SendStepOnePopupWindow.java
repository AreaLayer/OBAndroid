package com.omni.wallet.view.popupwindow.send;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;
import com.omni.wallet.R;
import com.omni.wallet.baselibrary.utils.LogUtils;
import com.omni.wallet.baselibrary.utils.PermissionUtils;
import com.omni.wallet.baselibrary.utils.StringUtils;
import com.omni.wallet.baselibrary.utils.ToastUtils;
import com.omni.wallet.baselibrary.view.BasePopWindow;
import com.omni.wallet.baselibrary.view.recyclerView.adapter.CommonRecyclerAdapter;
import com.omni.wallet.baselibrary.view.recyclerView.holder.ViewHolder;
import com.omni.wallet.entity.ListAssetItemEntity;
import com.omni.wallet.entity.event.SendSuccessEvent;
import com.omni.wallet.framelibrary.entity.User;
import com.omni.wallet.listItems.Friend;
import com.omni.wallet.listItems.FriendGroup;
import com.omni.wallet.ui.activity.ScanActivity;
import com.omni.wallet.view.dialog.LoadingDialog;
import com.omni.wallet.view.dialog.SendSuccessDialog;
import com.omni.wallet.view.popupwindow.SelectAssetPopupWindow;
import com.omni.wallet.view.popupwindow.SelectSpeedPopupWindow;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import lnrpc.LightningOuterClass;
import obdmobile.Callback;
import obdmobile.Obdmobile;

/**
 * SendStepOne的弹窗
 */
public class SendStepOnePopupWindow {
    private static final String TAG = SendStepOnePopupWindow.class.getSimpleName();

    private Context mContext;
    private BasePopWindow mBasePopWindow;
    private TextView sendFeeTv;
    private List<LightningOuterClass.Transaction> mData = new ArrayList<>();
    private MyAdapter mAdapter;
    View mView;
    private List<FriendGroup> friendGroups = new ArrayList<>();
    String selectAddress = "mhHHDCmkceKKXCFYCk7gRo5UaJmCi6rCk8";
    int time = 1;
    long feeStr;
    long assetId = 0;
    String assetBalance = "0";
    String assetBalanceMax;
    private LoadingDialog mLoadingDialog;
    // 初始数据（Initial data）
    String toFriendName = "Alice";
    SelectSpeedPopupWindow mSelectSpeedPopupWindow;
    SelectAssetPopupWindow mSelectAssetPopupWindow;
    SendSuccessDialog mSendSuccessDialog;

    public SendStepOnePopupWindow(Context context) {
        this.mContext = context;
    }

    /**
     * @描述： 测试使用数据
     * @description: data for test
     */
    public void friendGroupsData() {
        Friend alice = new Friend("Alice", "1mn8382odjd.........34gy7");
        Friend abbe = new Friend("Abbe", "2nm8382odjd.........dfe689");
        List<Friend> groupA = new ArrayList<Friend>();
        groupA.add(alice);
        groupA.add(abbe);
        FriendGroup friendGroupA = new FriendGroup("A", groupA);

        Friend bob = new Friend("Bob", "1mn8382odjd.........34gy7");
        Friend bill = new Friend("Bill", "2nm8382odjd.........dfe689");
        Friend boss = new Friend("Boss", "2nm8382odjd.........dfe689");
        List<Friend> groupB = new ArrayList<Friend>();
        groupB.add(bob);
        groupB.add(bill);
        groupB.add(boss);
        groupB.add(bob);
        groupB.add(bill);
        groupB.add(boss);
        FriendGroup friendGroupB = new FriendGroup("B", groupB);

        Friend charli = new Friend("Charli", "1mn8382odjd.........34gy7");
        List<Friend> groupC = new ArrayList<Friend>();
        groupC.add(charli);
        FriendGroup friendGroupC = new FriendGroup("C", groupC);

        friendGroups.add(friendGroupA);
        friendGroups.add(friendGroupB);
        friendGroups.add(friendGroupC);
    }


    public void show(final View view) {
        if (mBasePopWindow == null) {
            mView = view;
            mBasePopWindow = new BasePopWindow(mContext);
            final View rootView = mBasePopWindow.setContentView(R.layout.layout_popupwindow_send_stepone);
            mBasePopWindow.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
            mBasePopWindow.setHeight(WindowManager.LayoutParams.MATCH_PARENT);
            mBasePopWindow.setAnimationStyle(R.style.popup_anim_style);

            mLoadingDialog = new LoadingDialog(mContext);
            friendGroupsData();
            fetchWalletBalance();
            showStepOne(rootView);
            /**
             * @备注： 点击cancel 按钮
             * @description: Click cancel button
             */
            rootView.findViewById(R.id.layout_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBasePopWindow.dismiss();
                }
            });
            if (mBasePopWindow.isShowing()) {
                return;
            }
            mBasePopWindow.showAtLocation(mView, Gravity.CENTER, 0, 0);
        }
    }

    /**
     * send step one
     */
    private void showStepOne(View view) {
        /**
         * @description: RecyclerView for send list
         * @描述： send list 的 RecyclerView
         */
        TextView recentsAddressTv = view.findViewById(R.id.tv_recents_address);
        recentsAddressTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBasePopWindow.getContentView().findViewById(R.id.lv_step_one_content).setVisibility(View.GONE);
                mBasePopWindow.getContentView().findViewById(R.id.lv_step_two_content).setVisibility(View.VISIBLE);
                showStepTwo(view);
            }
        });
        RecyclerView mRecyclerView = view.findViewById(R.id.recycler_send_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        Obdmobile.getTransactions(LightningOuterClass.GetTransactionsRequest.newBuilder().build().toByteArray(), new Callback() {
            @Override
            public void onError(Exception e) {
                LogUtils.e(TAG, "------------------getTransactionsOnError------------------" + e.getMessage());
            }

            @Override
            public void onResponse(byte[] bytes) {
                if (bytes == null) {
                    return;
                }
                try {
                    LightningOuterClass.TransactionDetails resp = LightningOuterClass.TransactionDetails.parseFrom(bytes);
                    LogUtils.e(TAG, "------------------getTransactionsOnResponse-----------------" + resp);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            recentsAddressTv.setText(resp.getTransactions(0).getDestAddresses(0));
                            mData.clear();
                            mData.addAll(resp.getTransactionsList());
                            mAdapter = new MyAdapter(mContext, mData, R.layout.layout_item_send_list);
                            mRecyclerView.setAdapter(mAdapter);
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        });
        /**
         * @description: click scan icon
         * @描述： 点击scan
         */
        view.findViewById(R.id.iv_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionUtils.launchCamera((Activity) mContext, new PermissionUtils.PermissionCallback() {
                    @Override
                    public void onRequestPermissionSuccess() {
                        mBasePopWindow.dismiss();
                        Intent intent = new Intent(mContext, ScanActivity.class);
                        mContext.startActivity(intent);
                    }

                    @Override
                    public void onRequestPermissionFailure(List<String> permissions) {
                        LogUtils.e(TAG, "扫码页面摄像头权限拒绝");
                    }

                    @Override
                    public void onRequestPermissionFailureWithAskNeverAgain(List<String> permissions) {
                        LogUtils.e(TAG, "扫码页面摄像头权限拒绝并且勾选不再提示");
                    }
                });
            }
        });
    }

    /**
     * send step two
     */
    private void showStepTwo(View view) {
        TextView toAddressView = view.findViewById(R.id.tv_to_address);
        toAddressView.setText(selectAddress);
        TextView toFriendNameView = view.findViewById(R.id.tv_to_friend_name);
        toFriendNameView.setText(toFriendName);
        ImageView assetTypeIv = view.findViewById(R.id.iv_asset_type);
        TextView assetTypeTv = view.findViewById(R.id.tv_asset_type);
        TextView amountTypeTv = view.findViewById(R.id.tv_amount_type);
        TextView assetsBalanceTv = view.findViewById(R.id.tv_asset_balance);
        if (assetId == 0) {
            assetTypeIv.setImageResource(R.mipmap.icon_btc_logo_small);
            assetTypeTv.setText("BTC");
            amountTypeTv.setText("BTC");
        } else {
            assetTypeIv.setImageResource(R.mipmap.icon_usdt_logo_small);
            assetTypeTv.setText("USDT");
            amountTypeTv.setText("USDT");
        }
        assetsBalanceTv.setText(assetBalanceMax);
        sendFeeTv = view.findViewById(R.id.tv_send_fee);
        TextView sendFeeExchangeTv = view.findViewById(R.id.tv_send_fee_exchange);
        sendFeeExchangeTv.setText("0");
        LinearLayout assetLayout = view.findViewById(R.id.layout_asset);
        assetLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectAssetPopupWindow = new SelectAssetPopupWindow(mContext);
                mSelectAssetPopupWindow.setOnItemClickCallback(new SelectAssetPopupWindow.ItemCleckListener() {
                    @Override
                    public void onItemClick(View view, ListAssetItemEntity item) {
                        if (item.getPropertyid() == 0) {
                            assetTypeIv.setImageResource(R.mipmap.icon_btc_logo_small);
                            assetTypeTv.setText("BTC");
                            amountTypeTv.setText("BTC");
                        } else {
                            assetTypeIv.setImageResource(R.mipmap.icon_usdt_logo_small);
                            assetTypeTv.setText("USDT");
                            amountTypeTv.setText("USDT");
                        }
                        assetId = item.getPropertyid();
                        assetBalanceMax = item.getAmount() + "";
                        assetsBalanceTv.setText(assetBalanceMax);
                    }
                });
                mSelectAssetPopupWindow.show(v);
            }
        });
        /**
         * @描述: 增加MAX按钮的点击事件，点击将balance的值填入amount输入框中
         * @Description: Add the click event of MAX button, click to fill the balance value into the amount input box
         * @author: Tong ChangHui
         * @E-mail: tch081092@gmail.com
         */
        final EditText amountInputView = view.findViewById(R.id.etv_send_amount);
        amountInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                estimateOnChainFee(count, time);
            }

            @Override
            public void afterTextChanged(Editable s) {
                assetBalance = s.toString();
                if (!StringUtils.isEmpty(s.toString())) {
                    estimateOnChainFee(Long.parseLong(assetBalance), time);
                }
            }
        });
        TextView maxBtnView = view.findViewById(R.id.tv_btn_set_amount_max);
        maxBtnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                amountInputView.setText(assetBalanceMax);
            }
        });
        Button speedButton = view.findViewById(R.id.btn_speed);
        speedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectSpeedPopupWindow = new SelectSpeedPopupWindow(mContext);
                mSelectSpeedPopupWindow.setOnItemClickCallback(new SelectSpeedPopupWindow.ItemCleckListener() {
                    @Override
                    public void onItemClick(View view) {
                        switch (view.getId()) {
                            case R.id.tv_slow:
                                speedButton.setText(R.string.slow);
                                time = 1; // 10 Minutes
                                estimateOnChainFee(Long.parseLong(amountInputView.getText().toString()), time);
                                break;
                            case R.id.tv_medium:
                                speedButton.setText(R.string.medium);
                                time = 6 * 6; // 6 Hours
                                estimateOnChainFee(Long.parseLong(amountInputView.getText().toString()), time);
                                break;
                            case R.id.tv_fast:
                                speedButton.setText(R.string.fast);
                                time = 6 * 24; // 24 Hours
                                estimateOnChainFee(Long.parseLong(amountInputView.getText().toString()), time);
                                break;
                        }
                    }
                });
                mSelectSpeedPopupWindow.show(v);
            }
        });
        /**
         * @desc: Click back button then back to step one
         * @描述： 点击back显示step one
         */
        view.findViewById(R.id.layout_back_to_one).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.findViewById(R.id.lv_step_one_content).setVisibility(View.VISIBLE);
                view.findViewById(R.id.lv_step_two_content).setVisibility(View.GONE);
                showStepOne(view);
            }
        });
        /**
         * @desc: Click next button then forward to step three
         * @描述： 点击next跳转到step three
         */
        view.findViewById(R.id.layout_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StringUtils.isEmpty(assetBalance)) {
                    ToastUtils.showToast(mContext, mContext.getString(R.string.create_invoice_amount));
                    return;
                }
                if (assetBalance.equals("0")) {
                    ToastUtils.showToast(mContext, mContext.getString(R.string.amount_greater_than_0));
                    return;
                }
                if (Long.parseLong(assetBalance) - Long.parseLong(assetBalanceMax) > 0) {
                    ToastUtils.showToast(mContext, mContext.getString(R.string.credit_is_running_low));
                    return;
                }
                view.findViewById(R.id.lv_step_two_content).setVisibility(View.GONE);
                view.findViewById(R.id.lv_step_three_content).setVisibility(View.VISIBLE);
                showStepThree(view);
            }
        });
    }

    /**
     * send step three
     */
    private void showStepThree(View view) {
        /**
         * @描述: 初始化页面初始数据包括：friendName、address、send amount、send value、gas fee、fee value、total used value
         * @Description: The initial data of the initialization page includes friend name, address, send value, gas fee, fee value, total used value
         * @author: Tong ChangHui
         * @E-mail: tch081092@gmail.com
         */
        TextView friendNameView = view.findViewById(R.id.tv_send_friend_name);
        friendNameView.setText(toFriendName);
        TextView friendAddressView = view.findViewById(R.id.tv_send_address);
        friendAddressView.setText(selectAddress);
        ImageView tokenImage = view.findViewById(R.id.iv_send_token_image);
        TextView tokenTypeView = view.findViewById(R.id.tv_send_token_type);
        TextView tokenTypeView2 = view.findViewById(R.id.tv_send_token_type_2);
        if (assetId == 0) {
            tokenImage.setImageResource(R.mipmap.icon_btc_logo_small);
            tokenTypeView.setText("BTC");
            tokenTypeView2.setText("BTC");
        } else {
            tokenImage.setImageResource(R.mipmap.icon_usdt_logo_small);
            tokenTypeView.setText("USDT");
            tokenTypeView2.setText("USDT");
        }
        TextView sendAmountView = view.findViewById(R.id.tv_send_amount);
        sendAmountView.setText(assetBalance);
        TextView sendAmountValueView = view.findViewById(R.id.tv_send_amount_value);
        sendAmountValueView.setText(assetBalance);
        TextView feeAmountView = view.findViewById(R.id.tv_send_gas_fee_amount);
        feeAmountView.setText(feeStr + "");
        TextView feeAmountValueView = view.findViewById(R.id.tv_send_gas_fee_amount_value);
        feeAmountValueView.setText("0");
        TextView sendUsedValueView = view.findViewById(R.id.tv_send_used_value);
        long sendUsedValue = Long.parseLong(assetBalance) + feeStr;
        sendUsedValueView.setText(sendUsedValue + "");
        /**
         * @desc: Click back button then back to step two
         * @描述： 点击back显示step two
         */
        view.findViewById(R.id.layout_back_to_two).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.findViewById(R.id.lv_step_two_content).setVisibility(View.VISIBLE);
                view.findViewById(R.id.lv_step_three_content).setVisibility(View.GONE);
                showStepTwo(view);
            }
        });
        /**
         * @desc: Click confirm button then forward to succeed or failed step
         * @描述： 点击 confirm button 显示成功或者失败的页面
         */
        view.findViewById(R.id.layout_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtils.e(TAG, selectAddress);
                LogUtils.e(TAG, assetBalance);
                LogUtils.e(TAG, String.valueOf(time));
                LogUtils.e(TAG, String.valueOf(assetId));
                mLoadingDialog.show();
                if (assetId == 0) {
                    LightningOuterClass.SendCoinsFromRequest sendRequest = LightningOuterClass.SendCoinsFromRequest.newBuilder()
                            .setAddr(selectAddress)
                            .setFrom(User.getInstance().getWalletAddress(mContext))
                            .setAmount(Long.parseLong(assetBalance))
                            .setTargetConf(time)
                            .build();
                    Obdmobile.sendCoinsFrom(sendRequest.toByteArray(), new Callback() {
                        @Override
                        public void onError(Exception e) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    view.findViewById(R.id.lv_step_failed_content).setVisibility(View.VISIBLE);
                                    view.findViewById(R.id.lv_step_three_content).setVisibility(View.GONE);
                                    mLoadingDialog.dismiss();
                                    showStepFailed(view, e.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onResponse(byte[] bytes) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    EventBus.getDefault().post(new SendSuccessEvent());
                                    mLoadingDialog.dismiss();
                                    mBasePopWindow.dismiss();
                                    mSendSuccessDialog = new SendSuccessDialog(mContext);
                                    mSendSuccessDialog.show("success");
                                }
                            });
                        }
                    });
                } else {
                    LightningOuterClass.SendCoinsFromRequest sendRequest = LightningOuterClass.SendCoinsFromRequest.newBuilder()
                            .setAssetId((int) assetId)
                            .setAddr(selectAddress)
                            .setFrom(User.getInstance().getWalletAddress(mContext))
                            .setAssetAmount(Long.parseLong(assetBalance))
                            .setTargetConf(time)
                            .build();
                    Obdmobile.sendCoinsFrom(sendRequest.toByteArray(), new Callback() {
                        @Override
                        public void onError(Exception e) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    view.findViewById(R.id.lv_step_failed_content).setVisibility(View.VISIBLE);
                                    view.findViewById(R.id.lv_step_three_content).setVisibility(View.GONE);
                                    mLoadingDialog.dismiss();
                                    showStepFailed(view, e.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onResponse(byte[] bytes) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    EventBus.getDefault().post(new SendSuccessEvent());
                                    mLoadingDialog.dismiss();
                                    mBasePopWindow.dismiss();
                                    mSendSuccessDialog = new SendSuccessDialog(mContext);
                                    mSendSuccessDialog.show("success");
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    /**
     * send step failed
     */
    private void showStepFailed(View view, String message) {
        TextView failedNameTcv = view.findViewById(R.id.tv_failed_name);
        failedNameTcv.setText(toFriendName);
        TextView friendAddressTv = view.findViewById(R.id.tv_failed_address);
        friendAddressTv.setText(selectAddress);
        ImageView assetLogoIv = view.findViewById(R.id.iv_asset_logo);
        TextView assetUnitTv = view.findViewById(R.id.tv_asset_unit);
        TextView failedAmountUnitTv = view.findViewById(R.id.tv_failed_amount_unit);
        if (assetId == 0) {
            assetLogoIv.setImageResource(R.mipmap.icon_btc_logo_small);
            assetUnitTv.setText("BTC");
            failedAmountUnitTv.setText("BTC");
        } else {
            assetLogoIv.setImageResource(R.mipmap.icon_usdt_logo_small);
            assetUnitTv.setText("USDT");
            failedAmountUnitTv.setText("USDT");
        }
        TextView failedAmountTv = view.findViewById(R.id.tv_failed_amount);
        failedAmountTv.setText(assetBalance);
        TextView failedGasFeeTv = view.findViewById(R.id.tv_failed_gas_fee);
        failedGasFeeTv.setText(feeStr + "");
        TextView failedTotalValueTv = view.findViewById(R.id.tv_failed_total_value);
        long totalValue = Long.parseLong(assetBalance) + feeStr;
        failedTotalValueTv.setText(totalValue + "");
        TextView failedMessageTv = view.findViewById(R.id.tv_failed_message);
        failedMessageTv.setText(message);

        // 点击try again
        view.findViewById(R.id.layout_try_again).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoadingDialog.show();
                if (assetId == 0) {
                    LightningOuterClass.SendCoinsFromRequest sendRequest = LightningOuterClass.SendCoinsFromRequest.newBuilder()
                            .setAddr(selectAddress)
                            .setFrom(User.getInstance().getWalletAddress(mContext))
                            .setAmount(Long.parseLong(assetBalance))
                            .setTargetConf(time)
                            .build();
                    Obdmobile.sendCoinsFrom(sendRequest.toByteArray(), new Callback() {
                        @Override
                        public void onError(Exception e) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    view.findViewById(R.id.lv_step_failed_content).setVisibility(View.VISIBLE);
                                    view.findViewById(R.id.lv_step_three_content).setVisibility(View.GONE);
                                    mLoadingDialog.dismiss();
                                    failedMessageTv.setText(e.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onResponse(byte[] bytes) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    EventBus.getDefault().post(new SendSuccessEvent());
                                    mLoadingDialog.dismiss();
                                    mBasePopWindow.dismiss();
                                    mSendSuccessDialog = new SendSuccessDialog(mContext);
                                    mSendSuccessDialog.show("success");
                                }
                            });
                        }
                    });
                } else {
                    LightningOuterClass.SendCoinsFromRequest sendRequest = LightningOuterClass.SendCoinsFromRequest.newBuilder()
                            .setAssetId((int) assetId)
                            .setAddr(selectAddress)
                            .setFrom(User.getInstance().getWalletAddress(mContext))
                            .setAssetAmount(Long.parseLong(assetBalance))
                            .setTargetConf(time)
                            .build();
                    Obdmobile.sendCoinsFrom(sendRequest.toByteArray(), new Callback() {
                        @Override
                        public void onError(Exception e) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    view.findViewById(R.id.lv_step_failed_content).setVisibility(View.VISIBLE);
                                    view.findViewById(R.id.lv_step_three_content).setVisibility(View.GONE);
                                    mLoadingDialog.dismiss();
                                    failedMessageTv.setText(e.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onResponse(byte[] bytes) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    EventBus.getDefault().post(new SendSuccessEvent());
                                    mLoadingDialog.dismiss();
                                    mBasePopWindow.dismiss();
                                    mSendSuccessDialog = new SendSuccessDialog(mContext);
                                    mSendSuccessDialog.show("success");
                                }
                            });
                        }
                    });
                }
            }
        });
        // 点击explorer
        view.findViewById(R.id.layout_explorer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBasePopWindow.dismiss();
            }
        });
    }

    /**
     * @描述： send list列表适配器
     * @desc: Adapter for send list
     */
    private class MyAdapter extends CommonRecyclerAdapter<LightningOuterClass.Transaction> {

        public MyAdapter(Context context, List<LightningOuterClass.Transaction> data, int layoutId) {
            super(context, data, layoutId);
        }

        @Override
        public void convert(ViewHolder holder, final int position, final LightningOuterClass.Transaction item) {
//            holder.setText(R.id.tv_group_name, item.getGroupName());
//            holder.getView(R.id.v_deliver).setVisibility(View.INVISIBLE);
//
//            LinearLayout ListContentView = holder.getView(R.id.lv_friend_item_list);
//
//            List<Friend> friendListInGroup = item.getGroupFriend();
//
//            for (int i = 0; i < friendListInGroup.size(); i++) {
//                String friendName = friendListInGroup.get(i).getFriendName();
//                String address = friendListInGroup.get(i).getAddress();
//
//                RelativeLayout friendItemContain = new RelativeLayout(mContext);
//                RelativeLayout.LayoutParams friendItemContainParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//                friendItemContain.setLayoutParams(friendItemContainParams);
//
//                LinearLayout friendItemContent = new LinearLayout(mContext);
//                LinearLayout.LayoutParams friendItemContentParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                friendItemContent.setPadding(0, 20, 0, 20);
//                friendItemContent.setOrientation(LinearLayout.HORIZONTAL);
//                friendItemContent.setLayoutParams(friendItemContentParams);
//
//                TextView friendNameView = new TextView(mContext);
//                LinearLayout.LayoutParams friendNameViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                friendNameView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
//                friendNameView.setGravity(Gravity.CENTER);
//                friendNameView.setTextSize(16);
//                friendNameView.setTextColor(mContext.getResources().getColor(R.color.color_99_transparent));
//                friendNameView.setText(friendName);
//                friendNameView.setLayoutParams(friendNameViewParams);
//
//                TextView friendAddressView = new TextView(mContext);
//                LinearLayout.LayoutParams friendAddressViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
//                friendAddressView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
//                friendAddressView.setGravity(Gravity.CENTER);
//                friendAddressView.setTextSize(16);
//                friendAddressView.setTextColor(mContext.getResources().getColor(R.color.color_99_transparent));
//                friendAddressView.setText(address);
//                friendAddressView.setLayoutParams(friendAddressViewParams);
//
//                friendItemContent.addView(friendNameView);
//                friendItemContent.addView(friendAddressView);
//                friendItemContain.addView(friendItemContent);
//                friendItemContent.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        mBasePopWindow.getContentView().findViewById(R.id.lv_step_one_content).setVisibility(View.GONE);
//                        mBasePopWindow.getContentView().findViewById(R.id.lv_step_two_content).setVisibility(View.VISIBLE);
//                    }
//                });
//                ListContentView.addView(friendItemContain);
//            }
            holder.setOnItemClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBasePopWindow.getContentView().findViewById(R.id.lv_step_one_content).setVisibility(View.GONE);
                    mBasePopWindow.getContentView().findViewById(R.id.lv_step_two_content).setVisibility(View.VISIBLE);
                    selectAddress = item.getDestAddresses(0);
                }
            });
            holder.setText(R.id.tv_send_list_address, item.getDestAddresses(0));
        }
    }

    /**
     * calculate fee
     */
    private void estimateOnChainFee(long amount, int targetConf) {
        // let LND estimate fee
        LightningOuterClass.EstimateFeeRequest asyncEstimateFeeRequest = LightningOuterClass.EstimateFeeRequest.newBuilder()
                .putAddrToAmount(selectAddress, amount)
                .setTargetConf(targetConf)
                .build();
        Obdmobile.estimateFee(asyncEstimateFeeRequest.toByteArray(), new Callback() {
            @Override
            public void onError(Exception e) {
                LogUtils.e(TAG, "------------------asyncEstimateFeeOnError------------------" + e.getMessage());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        feeStr = 0;
                        sendFeeTv.setText(feeStr + "");
                    }
                });
            }

            @Override
            public void onResponse(byte[] bytes) {
                if (bytes == null) {
                    return;
                }
                try {
                    LightningOuterClass.EstimateFeeResponse resp = LightningOuterClass.EstimateFeeResponse.parseFrom(bytes);
                    LogUtils.e(TAG, "------------------asyncEstimateFeeOnResponse-----------------" + resp);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            feeStr = resp.getFeeSat();
                            sendFeeTv.setText(feeStr + "");
                        }
                    });
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create a new wallet address first, and then request the interface of each asset balance list
     * 先创建新的钱包地址后再去请求各资产余额列表的接口
     */
    public void fetchWalletBalance() {
        LightningOuterClass.NewAddressRequest asyncNewAddressRequest = LightningOuterClass.NewAddressRequest.newBuilder()
                .setTypeValue(2)
                .build();
        Obdmobile.newAddress(asyncNewAddressRequest.toByteArray(), new Callback() {
            @Override
            public void onError(Exception e) {
                LogUtils.e(TAG, "------------------newAddressOnError------------------" + e.getMessage());
            }

            @Override
            public void onResponse(byte[] bytes) {
                if (bytes == null) {
                    return;
                }
                try {
                    LightningOuterClass.NewAddressResponse addressResp = LightningOuterClass.NewAddressResponse.parseFrom(bytes);
                    LogUtils.e(TAG, "------------------newAddressOnResponse-----------------" + addressResp.getAddress());
                    LightningOuterClass.WalletBalanceByAddressRequest walletBalanceByAddressRequest = LightningOuterClass.WalletBalanceByAddressRequest.newBuilder()
                            .setAddress(User.getInstance().getWalletAddress(mContext))
                            .build();
                    Obdmobile.walletBalanceByAddress(walletBalanceByAddressRequest.toByteArray(), new Callback() {
                        @Override
                        public void onError(Exception e) {
                            LogUtils.e(TAG, "------------------walletBalanceByAddressOnError------------------" + e.getMessage());
                        }

                        @Override
                        public void onResponse(byte[] bytes) {
                            if (bytes == null) {
                                return;
                            }
                            try {
                                LightningOuterClass.WalletBalanceByAddressResponse resp = LightningOuterClass.WalletBalanceByAddressResponse.parseFrom(bytes);
                                LogUtils.e(TAG, "------------------walletBalanceByAddressOnResponse-----------------" + resp);
                                assetBalanceMax = resp.getConfirmedBalance() + "";
                            } catch (InvalidProtocolBufferException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void release() {
        if (mBasePopWindow != null) {
            mBasePopWindow.dismiss();
            mBasePopWindow = null;
        }
    }
}
