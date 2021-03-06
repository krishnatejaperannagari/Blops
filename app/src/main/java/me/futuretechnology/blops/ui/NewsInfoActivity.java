/*
* Copyright 2015 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package me.futuretechnology.blops.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;
import org.xml.sax.XMLReader;
import me.futuretechnology.blops.R;
import me.futuretechnology.blops.data.DB;
import me.futuretechnology.blops.data.DbProvider;
import me.futuretechnology.blops.ui.util.LoaderIds;
import me.futuretechnology.util.DateTime;
import me.futuretechnology.util.Log;
import me.futuretechnology.util.ui.AsyncLoader;
import me.futuretechnology.util.ui.LoaderPayload;

public class NewsInfoActivity extends BaseActivity {
    public static final String EXTRA_ID = "me.futuretechnology.blops.newsinfo.id";
    public static final String EXTRA_FEED_ID = "me.futuretechnology.blops.newsinfo.feed_id";
    public static final String EXTRA_AUTHOR = "me.futuretechnology.blops.newsinfo.author";
    public static final String EXTRA_TITLE = "me.futuretechnology.blops.newsinfo.title";
    public static final String EXTRA_DATE = "me.futuretechnology.blops.newsinfo.date";
    public static final String EXTRA_CONTENT_VALUES = "me.futuretechnology.blops.newsinfo.news_content";
    public static final String EXTRA_URL = "me.futuretechnology.blops.newsinfo.url";
    public static final String EXTRA_IS_BOOKMARK = "me.futuretechnology.blops.newsinfo.is_bookmark";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_news_info;
    }

    @Override
    protected Toolbar initToolbar() {
        Toolbar toolbar = super.initToolbar();
        if (toolbar != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.setTitle(getIntent().getStringExtra(EXTRA_FEED_ID));
        }
        return toolbar;
    }

    @Override
    protected void initUI() {
        super.initUI();

        Intent intent = getIntent();

        StringBuilder txtCaption = new StringBuilder(128);
        String author = intent.getStringExtra(EXTRA_AUTHOR);
        if (!TextUtils.isEmpty(author)) {
            txtCaption.append(author);
            txtCaption.append('\n');
        }
        txtCaption.append(DateTime.formatDateTime(this, intent.getLongExtra(EXTRA_DATE, 0), ", "));

        TextView tvDate = (TextView) findViewById(R.id.news_caption);
        tvDate.setText(txtCaption);

        TextView tvTitle = (TextView) findViewById(R.id.news_title);
        tvTitle.setText(intent.getStringExtra(EXTRA_TITLE));

        TextView tvContent = (TextView) findViewById(R.id.news_content);

        String txt = intent.getStringExtra(EXTRA_CONTENT_VALUES);
        tvContent.setText(Html.fromHtml(txt, null, new HtmlTagHandler()));

        // tvContent.setText(Html.fromHtml("Hello € <b>world</b>!<br>This is only a <a href=\"http://www.google.com\">test</a>."));
        tvContent.setMovementMethod(LinkMovementMethod.getInstance());

        // Log.i("BLOPS", "types: ", intent.getStringExtra(EXTRA_CONTENT_TYPES));
        Log.i("BLOPS", "values: ", txt);
        // Log.i("BLOPS", "url: ", intent.getStringExtra(EXTRA_URL));
        // Log.i("BLOPS", "id: ", intent.getStringExtra(EXTRA_ID));

        final String url = getIntent().getStringExtra(EXTRA_URL);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_open_in_browser);

        if (TextUtils.isEmpty(url)) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent iBrowser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    iBrowser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(iBrowser);
                }
            });
            fab.attachToScrollView((ObservableScrollView) findViewById(R.id.news_container));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.opt_news_info, menu);

        boolean isStarred = getIntent().getBooleanExtra(EXTRA_IS_BOOKMARK, false);
        menu.findItem(R.id.menu_bookmark_outline).setVisible(!isStarred);
        menu.findItem(R.id.menu_bookmark).setVisible(isStarred);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_bookmark_outline:
            case R.id.menu_bookmark:
                Bundle b = new Bundle();
                b.putBoolean("is_bookmark", item.getItemId() == R.id.menu_bookmark);

                LoaderManager lm = getSupportLoaderManager();
                lm.restartLoader(LoaderIds.ASYNC_STAR, b, loaderCallbacks);
                break;
            case R.id.menu_share:
                Intent iShare = new Intent(Intent.ACTION_SEND);
                iShare.setType("text/plain");

                StringBuilder sbText = new StringBuilder(256);
                sbText.append(getIntent().getStringExtra(EXTRA_TITLE));
                sbText.append('\n');
                sbText.append(getIntent().getStringExtra(EXTRA_URL));
                sbText.append('\n');
                sbText.append('\n');
                sbText.append(getText(R.string.app_name));
                sbText.append('\n');
                sbText.append("http://goo.gl/uKAO0");
                iShare.putExtra(Intent.EXTRA_TEXT, sbText.toString());

                startActivity(Intent.createChooser(iShare, getText(R.string.menu_share)));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private final LoaderManager.LoaderCallbacks<LoaderPayload> loaderCallbacks = new LoaderCallbacks<LoaderPayload>() {
        @Override
        public Loader<LoaderPayload> onCreateLoader(int id, final Bundle data) {
            switch (id) {
                case LoaderIds.ASYNC_STAR:
                    return new AsyncLoader<LoaderPayload>(NewsInfoActivity.this) {
                        @Override
                        public LoaderPayload loadInBackground() {
                            String newsId = getIntent().getStringExtra(EXTRA_ID);
                            String feedId = getIntent().getStringExtra(EXTRA_FEED_ID);

                            ContentValues v = new ContentValues(2);
                            v.put(DB.MyNews.IS_BOOKMARK, data.getBoolean("is_bookmark") ? 0 : 1);

                            if (DbProvider.contentResolver.update(DB.MyNews.URI, v,
                                    DB.MyNews.ID + "=? AND " + DB.MyNews.FEED_ID + "=?",
                                    new String[]{newsId, feedId}) > 0) {
                                DbProvider.contentResolver.notifyChange(DB.MyNews.URI, null, false);
                                return new LoaderPayload(LoaderPayload.STATUS_OK,
                                        data.getBoolean("is_bookmark") ? 0 : 1);
                            }

                            return new LoaderPayload(LoaderPayload.STATUS_ERROR,
                                    data.getBoolean("is_bookmark") ? 1 : 0);
                        }
                    };
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<LoaderPayload> loader, LoaderPayload payload) {
            switch (loader.getId()) {
                case LoaderIds.ASYNC_STAR:
                    getIntent().putExtra(EXTRA_IS_BOOKMARK, payload.getReason() > 0);
                    invalidateOptionsMenu();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<LoaderPayload> loader) {

        }
    };

    // this adds support for ordered and unordered lists to Html.fromHtml()
    private class HtmlTagHandler implements Html.TagHandler {
        int index;

        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader reader) {
            if (opening && tag.equals("ul")) {
                index = -1;
            } else if (opening && tag.equals("ol")) {
                index = 1;
            } else if (tag.equals("li")) {
                if (opening) {
                    if (index < 0) {
                        output.append("\t• ");
                    } else {
                        output.append("\t");
                        output.append(String.valueOf(index));
                        output.append(". ");
                        ++index;
                    }
                } else {
                    output.append('\n');
                }
            }
        }
    }
}
