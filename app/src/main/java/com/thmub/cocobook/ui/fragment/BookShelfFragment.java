package com.thmub.cocobook.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import butterknife.BindView;
import com.thmub.cocobook.R;
import com.thmub.cocobook.manager.RxBusManager;
import com.thmub.cocobook.model.bean.CollBookBean;
import com.thmub.cocobook.model.event.*;
import com.thmub.cocobook.model.local.BookRepository;
import com.thmub.cocobook.presenter.BookShelfPresenter;
import com.thmub.cocobook.presenter.contract.BookShelfContract;
import com.thmub.cocobook.ui.activity.ReadActivity;
import com.thmub.cocobook.ui.activity.SearchActivity;
import com.thmub.cocobook.ui.adapter.CollBookAdapter;
import com.thmub.cocobook.base.BaseMVPFragment;
import com.thmub.cocobook.utils.*;
import com.thmub.cocobook.widget.adapter.WholeAdapter;
import com.thmub.cocobook.widget.itemdecoration.DividerItemDecoration;
import com.thmub.cocobook.widget.refresh.ScrollRefreshRecyclerView;

import java.io.File;
import java.util.List;

/**
 * Created by zhouas666 on 18-1-23.
 * 书架fragment
 */

public class BookShelfFragment extends BaseMVPFragment<BookShelfContract.Presenter>
        implements BookShelfContract.View {

    @BindView(R.id.book_shelf_rv_content)
    ScrollRefreshRecyclerView mRvContent;
    //全选
//    @BindView(R.id.multi_select_rl_root)
//    RelativeLayout multiSelectRlRoot;
//    @BindView(R.id.multi_select_cb_all)
//    CheckBox multiSelectCbAll;
//    @BindView(R.id.multi_select_btn_add)
//    Button multiSelectBtnAdd;
//    @BindView(R.id.multi_select_btn_delete)
//    Button multiSelectBtnDelete;

    /***************************视图********************************/
    private CollBookAdapter mCollBookAdapter;
    private FooterItemView mFooterItem;

    /***************************参数********************************/
    //是否是第一次进入
    private boolean isInit = true;

    /***************************公共方法********************************/


    public void refreshShelf(){
        mCollBookAdapter.refreshItems(BookRepository.getInstance().getCollBooks());
    }
    /***************************初始化********************************/
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_bookshelf;
    }

    @Override
    protected BookShelfContract.Presenter bindPresenter() {
        return new BookShelfPresenter();
    }

    @Override
    protected void initWidget(Bundle savedInstanceState) {
        super.initWidget(savedInstanceState);
        initAdapter();
        initEvent();
    }

    private void initAdapter() {
        //添加Footer
        mCollBookAdapter = new CollBookAdapter();
        mRvContent.setLayoutManager(new LinearLayoutManager(mContext));
        mRvContent.addItemDecoration(new DividerItemDecoration(mContext));
        mRvContent.setAdapter(mCollBookAdapter);
    }

    /**
     * 初始化事件
     */
    private void initEvent() {

        //接收Read页面传来的下载消息
        addDisposable(RxBusManager.getInstance()
                .toObservable(DownloadEvent.class)
                .compose(RxUtils::toSimpleSingle)
                .subscribe(
                        event -> mPresenter.createDownloadTask(event.collBook)
                ));

        //删除书籍
        addDisposable(RxBusManager.getInstance()
                .toObservable(DeleteResponseEvent.class)
                .compose(RxUtils::toSimpleSingle)
                .subscribe(event -> {
                            if (event.isDelete) {
                                ProgressUtils.show(mContext, "正在删除中");
                                addDisposable(BookRepository.getInstance().deleteCollBookInRx(event.collBook)
                                        .compose(RxUtils::toSimpleSingle)
                                        .subscribe(
                                                (integer) -> {
                                                    mCollBookAdapter.removeItem(event.collBook);
                                                    ProgressUtils.dismiss();
                                                }, throwable -> {
                                                    ProgressUtils.dismiss();
                                                }
                                        ));
                            } else {
                                //弹出一个Dialog
                                AlertDialog tipDialog = new AlertDialog.Builder(mContext)
                                        .setTitle("您的任务正在加载")
                                        .setMessage("先请暂停任务再进行删除")
                                        .setPositiveButton("确定", (dialogInterface, which) -> {
                                            dialogInterface.dismiss();
                                        }).create();
                                tipDialog.show();
                            }
                        }
                ));
    }

    @Override
    protected void initClick() {
        super.initClick();

        mRvContent.setOnRefreshListener(
                () -> mPresenter.updateCollBooks(mCollBookAdapter.getItems())
        );

        mCollBookAdapter.setOnItemClickListener(
                (view, pos) -> {
                    //如果是本地文件，首先判断这个文件是否存在
                    CollBookBean collBook = mCollBookAdapter.getItem(pos);
                    if (collBook.isLocal()) {
                        //id表示本地文件的路径
                        String path = collBook.getCover();
                        File file = new File(path);
                        //判断这个本地文件是否存在
                        if (file.exists()) {
                            ReadActivity.startActivity(mContext,
                                    mCollBookAdapter.getItem(pos), true);
                        } else {
                            //提示(从目录中移除这个文件)
                            new AlertDialog.Builder(mContext)
                                    .setTitle(UiUtils.getString(R.string.common_tip))
                                    .setMessage("文件不存在,是否删除")
                                    .setPositiveButton(getResources().getString(R.string.common_sure),
                                            ((dialogInterface, i) -> {
                                                deleteBook(collBook);
                                            }))
                                    .setNegativeButton(UiUtils.getString(R.string.common_cancel), null)
                                    .show();
                        }
                    } else {
                        ReadActivity.startActivity(mContext,
                                mCollBookAdapter.getItem(pos), true);
                    }
                }
        );

        mCollBookAdapter.setOnItemLongClickListener(
                (v, pos) -> {
                    //开启Dialog,最方便的Dialog,就是AlterDialog
                    openItemDialog(mCollBookAdapter.getItem(pos));
                    return true;
                }
        );
    }

    /***************************业务逻辑********************************/
    @Override
    protected void processLogic() {
        super.processLogic();
        if(NetworkUtils.isConnected()){
            mRvContent.startRefresh();
            //推荐图书
            if (!SharedPreUtils.getInstance().getBoolean(Constant.SHARED_RECOMMENDED)&&
                    SharedPreUtils.getInstance().getString(Constant.SHARED_SEX)!=null){
                mPresenter.loadRecommendBooks(SharedPreUtils.getInstance().getString(Constant.SHARED_SEX));
                SharedPreUtils.getInstance().putBoolean(Constant.SHARED_RECOMMENDED,true);
            }
        }else {
            SnackbarUtils.show(mContext,"当前网络不可用");
        }
    }

    @Override
    public void showError() {

    }

    @Override
    public void complete() {
        if (mCollBookAdapter.getItemCount() > 0 && mFooterItem == null) {
            mFooterItem = new FooterItemView();
            mCollBookAdapter.addFooterView(mFooterItem);
        }

        if (mRvContent.isRefreshing()) {
            mRvContent.finishRefresh();
        }
    }

    @Override
    public void finishRefresh(List<CollBookBean> collBookBeans) {
        mCollBookAdapter.refreshItems(collBookBeans);
        //如果是初次进入，则更新书籍信息
        if (isInit) {
            isInit = false;
            mRvContent.post(
                    () -> mPresenter.updateCollBooks(mCollBookAdapter.getItems())
            );
        }
    }

    @Override
    public void finishUpdate() {
        //重新从数据库中获取数据
        Log.e(TAG,"------------finishUpdate");
        mCollBookAdapter.refreshItems(BookRepository.getInstance().getCollBooks());
    }

    @Override
    public void showErrorTip(String error) {
        mRvContent.setTip(error);
        mRvContent.showTip();
    }


    /***************************事件处理********************************/
    private void openItemDialog(CollBookBean collBook) {
        String[] menus = collBook.isLocal() ? UiUtils.getStringArray(R.array.menu_local_book)
                : UiUtils.getStringArray(R.array.menu_net_book);
        AlertDialog collBookDialog = new AlertDialog.Builder(mContext)
                .setTitle(collBook.getTitle())
                .setAdapter(new ArrayAdapter<String>(mContext,
                                android.R.layout.simple_list_item_1, menus),
                        (dialog, which) -> onItemMenuClick(menus[which], collBook))
                .create();

        collBookDialog.show();
    }

    private void onItemMenuClick(String which, CollBookBean collBook) {
        switch (which) {
            case "置顶":
                SnackbarUtils.show(mContext,"此功能尚未完成");
                break;
            case "缓存":
                downloadBook(collBook);
                break;
            case "删除":
                deleteBook(collBook);
                break;
            case "批量管理":
                SnackbarUtils.show(mContext,"此功能尚未完成");
                break;
            default:
                break;
        }
    }


    private void downloadBook(CollBookBean collBook) {
        //创建任务
        mPresenter.createDownloadTask(collBook);
    }

    /**
     * 默认删除本地文件
     *
     * @param collBook
     */
    private void deleteBook(CollBookBean collBook) {

        if (collBook.isLocal()) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_delete, null);
            CheckBox cb = (CheckBox) view.findViewById(R.id.delete_cb_select);
            new AlertDialog.Builder(mContext)
                    .setTitle("删除文件")
                    .setView(view)
                    .setPositiveButton(UiUtils.getString(R.string.common_sure), ((dialogInterface, i) -> {
                        boolean isSelected = cb.isSelected();
                        if (isSelected) {
                            ProgressUtils.show(mContext, "删除中...");
                            //删除
                            File file = new File(collBook.getCover());
                            if (file.exists()) file.delete();
                            BookRepository.getInstance().deleteCollBook(collBook);
                            BookRepository.getInstance().deleteBookRecord(collBook.get_id());

                            //从Adapter中删除
                            mCollBookAdapter.removeItem(collBook);
                            ProgressUtils.dismiss();
                        } else {
                            BookRepository.getInstance().deleteCollBook(collBook);
                            BookRepository.getInstance().deleteBookRecord(collBook.get_id());
                            //从Adapter中删除
                            mCollBookAdapter.removeItem(collBook);
                        }
                    }))
                    .setNegativeButton(getResources().getString(R.string.common_cancel), null)
                    .show();
        } else {
            RxBusManager.getInstance().post(new DeleteTaskEvent(collBook));
        }
    }

    /*****************************************************************/
    class FooterItemView implements WholeAdapter.ItemView {
        @Override
        public View onCreateView(ViewGroup parent) {
            View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.footer_book_shelf, parent, false);
            view.setOnClickListener((v) -> {
                        startActivity(new Intent(mContext, SearchActivity.class));
                    }
            );
            return view;
        }

        @Override
        public void onBindView(View view) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.refreshCollBooks();
    }
}
