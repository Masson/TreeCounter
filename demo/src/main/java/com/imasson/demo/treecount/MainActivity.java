package com.imasson.demo.treecount;

import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.imasson.lib.treecounter.CountNode;
import com.imasson.lib.treecounter.TreeCounter;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CountNodeAdapter adapter;
    private TreeCountWrapper treeCountWrapper = new TreeCountWrapper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                treeCountWrapper.getTreeCounter().reset();
                Snackbar.make(view, "The TreeCounter has been reset.", Snackbar.LENGTH_SHORT).show();
            }
        });

        treeCountWrapper.getTreeCounter().setListener(
                new TreeCounter.OnNodeUnreadCountChangeListener() {
            @Override
            public void onNodeUnreadCountChanged(CountNode node) {
                if (node == treeCountWrapper.getTreeCounter().getRootNode()) {
                    CollapsingToolbarLayout toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
                    toolbarLayout.setTitle(getString(R.string.activity_title_pattern,
                            treeCountWrapper.getTreeCounter().getCount()));
                } else {
                    adapter.notifyDataSetChanged();
                }
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CountNodeAdapter(treeCountWrapper.getTreeCounter());
        recyclerView.setAdapter(adapter);

        setTitle(getString(R.string.activity_title_pattern,
                treeCountWrapper.getTreeCounter().getCount()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private static class CountNodeAdapter extends RecyclerView.Adapter<CountNodeAdapter.ViewHolder>
            implements View.OnClickListener {

        private List<CountNode> mCountNodeList;
        private TreeCounter mTreeCounter;

        public CountNodeAdapter(TreeCounter treeCounter) {
            mTreeCounter = treeCounter;
            refresh();
        }

        public void refresh() {
            mCountNodeList = mTreeCounter.generateCountNodeList();
            mCountNodeList.remove(0);  // remove root
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_view_count_node, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            viewHolder.touchButton.setOnClickListener(this);
            viewHolder.addButton.setOnClickListener(this);
            viewHolder.reduceButton.setOnClickListener(this);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            CountNode countNode = mCountNodeList.get(position);
            holder.title.setText(countNode.getId());
            holder.summary.setText(countNode.getDebugInfo());
            holder.displayCount.setText(String.valueOf(countNode.getDisplayCount()));
            holder.actualCount.setText(String.valueOf(countNode.getCount()));
            holder.layoutCountOperation.setVisibility(countNode.isLeaf() ? View.VISIBLE : View.GONE);
            if (countNode.getDepth() == 1) {
                holder.spacer.setText("");
            } else if (countNode.getDepth() == 2) {
                holder.spacer.setText("        ");
            } else {
                holder.spacer.setText("                ");
            }
            holder.touchButton.setTag(countNode);
            holder.addButton.setTag(countNode);
            holder.reduceButton.setTag(countNode);
        }

        @Override
        public int getItemCount() {
            return mCountNodeList.size();
        }

        @Override
        public void onClick(View v) {
            CountNode countNode = (CountNode) v.getTag();
            switch (v.getId()) {
                case R.id.btn_add:
                    mTreeCounter.addCount(countNode.getId(), 1);
                    break;
                case R.id.btn_reduce:
                    mTreeCounter.reduceCount(countNode.getId(), 1);
                    break;
                case R.id.btn_touch:
                    mTreeCounter.touchNode(countNode.getId());
                    break;
            }
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {

            public TextView title;
            public TextView summary;
            public TextView displayCount;
            public TextView actualCount;
            public Button touchButton;
            public View layoutCountOperation;
            public Button addButton;
            public Button reduceButton;
            public TextView spacer;

            public ViewHolder(View itemView) {
                super(itemView);
                title = (TextView) itemView.findViewById(R.id.item_title);
                summary = (TextView) itemView.findViewById(R.id.item_summary);
                displayCount = (TextView) itemView.findViewById(R.id.item_display_count);
                actualCount = (TextView) itemView.findViewById(R.id.item_actual_count);
                touchButton = (Button) itemView.findViewById(R.id.btn_touch);
                addButton = (Button) itemView.findViewById(R.id.btn_add);
                reduceButton = (Button) itemView.findViewById(R.id.btn_reduce);
                layoutCountOperation = itemView.findViewById(R.id.layout_count_operation);
                spacer = (TextView) itemView.findViewById(R.id.spacer);
            }
        }
    }
}
