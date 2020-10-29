package cn.archko.pdf.tree;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cn.archko.pdf.R;
import cn.archko.pdf.listeners.OnItemClickListener;

/**
 * @author: archko 2020/10/29 :15:29
 */
public class TreeAdapter extends RecyclerView.Adapter<TreeAdapter.ViewHolder> {
    private ArrayList<Tree> treeList = new ArrayList<>();
    private Context context;
    private Tree pin;
    private LayoutInflater layoutInflater;
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public TreeAdapter(Context context, Tree tree) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        treeList.addAll(tree.child);
    }

    @NonNull
    @Override
    public TreeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(layoutInflater.inflate(R.layout.item_tree_outline, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TreeAdapter.ViewHolder holder, int position) {
        Tree tree = treeList.get(position);
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < tree.level; i++) {
            indent.append(" ");
        }
        if (!tree.child.isEmpty()) {
            holder.tag.setText(String.format("%s%s", indent, tree.tag ? "-" : "+"));
        } else {
            holder.tag.setText(indent);
        }
        holder.node.setText(tree.node);
        holder.page.setText(tree.page);
        holder.itemView.setOnClickListener(view -> {
            int pos = holder.getLayoutPosition();
            pin = treeList.get(pos);
            pin.tag = !pin.tag;
            if (pin.tag) {
                if (pin.child.size() == 0) {
                    if (null != onItemClickListener) {
                        onItemClickListener.onItemClick(view, pin.page, position);
                    }
                    return;
                }
                expand(pos);
            } else {
                fold(pos);
            }
            notifyDataSetChanged();
        });
    }

    private void fold(int pos) {
        Stack<Tree> stack = new Stack<>();
        stack.push(treeList.get(pos));
        int count = 0;
        while (!stack.isEmpty()) {
            for (Tree tree : stack.pop().child) {
                if (tree.tag) {
                    stack.push(tree);
                }
                count++;
            }
        }
        for (int i = 0; i < count; i++) {
            treeList.remove(pos + 1);
        }
    }

    private void expand(int pos) {
        treeList.addAll(pos + 1, treeList.get(pos).child);
    }

    @Override
    public int getItemCount() {
        return treeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView node;
        TextView tag;
        TextView page;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tag = itemView.findViewById(R.id.tag);
            node = itemView.findViewById(R.id.node);
            page = itemView.findViewById(R.id.page);
        }
    }
}