package com.imptt.apm29.ui.ptt.ui.contact

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.imptt.apm29.R
import kotlinx.android.synthetic.main.fragment_contact.*


class ContactFragment : Fragment() {

    private lateinit var contactViewModel: ContactViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        contactViewModel =
            ViewModelProviders.of(this).get(ContactViewModel::class.java)
        return inflater.inflate(R.layout.fragment_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contactViewModel.expandableData.observe(requireActivity()) {
            expandableListView.setAdapter(
                ContactAdapter(it)
            )
        }
        contactViewModel.initData()

    }

    inner class ContactAdapter(private val expandableData: ContactViewModel.ExpandableData) :
        BaseExpandableListAdapter() {
        override fun getGroupCount(): Int {
            return expandableData.groups.size
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            return expandableData.items[groupPosition].size
        }

        override fun getGroup(groupPosition: Int): Any {
            return expandableData.groups[groupPosition]
        }

        override fun getChild(groupPosition: Int, childPosition: Int): Any {
            return expandableData.items[groupPosition][childPosition]
        }

        override fun getGroupId(groupPosition: Int): Long {
            return groupPosition.toLong()
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return expandableData.items[groupPosition][childPosition].hashCode().toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        @SuppressLint("SetTextI18n")
        override fun getGroupView(
            groupPosition: Int,
            isExpanded: Boolean,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            val itemView = convertView ?: layoutInflater.inflate(
                R.layout.contact_group_layout,
                parent,
                false
            )
            val group: String = expandableData.groups[groupPosition]
            val count: Int = expandableData.items[groupPosition].size
            val tvGroup = itemView.findViewById(R.id.textViewGroupName) as TextView
            val tvGroupCount = itemView.findViewById(R.id.textViewGroupContactCount) as TextView
            tvGroup.text = group
            tvGroupCount.text = "${count}人"
            return itemView
        }

        override fun getChildView(
            groupPosition: Int,
            childPosition: Int,
            isLastChild: Boolean,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            val itemView = convertView ?: layoutInflater.inflate(
                R.layout.contact_item_layout,
                parent,
                false
            )
            val child: String = expandableData.items[groupPosition][childPosition]
            val textViewContactName = itemView.findViewById(R.id.textViewContactName) as TextView
            val textViewContactSubtitle =
                itemView.findViewById(R.id.textViewContactSubtitle) as TextView
            textViewContactName.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    child,
                    Toast.LENGTH_SHORT
                ).show()
            }
            textViewContactName.text = child
            textViewContactSubtitle.text = "在线"
            return itemView
        }

        override fun isChildSelectable(
            groupPosition: Int,
            childPosition: Int
        ): Boolean {
            return true
        }

    }
}