    package com.example.clockit;

    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.TextView;

    import androidx.annotation.NonNull;
    import androidx.recyclerview.widget.RecyclerView;

    import java.util.List;

    public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

        private final List<ClassModel> classList;
        private final OnClassSelectedListener listener;

        public ClassAdapter(List<ClassModel> classList, OnClassSelectedListener listener) {
            this.classList = classList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ClassModel classModel = classList.get(position);
            holder.tvClassName.setText(classModel.getClassName());
            holder.tvClassDetails.setText(classModel.getClassCode() + " - " + classModel.getTeacherName());
            holder.itemView.setOnClickListener(v -> listener.onClassSelected(classModel));
        }

        @Override
        public int getItemCount() {
            return classList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvClassName, tvClassDetails;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClassName = itemView.findViewById(R.id.tvClassName);
                tvClassDetails = itemView.findViewById(R.id.tvClassDetails);
            }
        }

        public interface OnClassSelectedListener {
            void onClassSelected(ClassModel classModel);
        }
    }
