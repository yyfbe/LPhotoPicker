package top.limuyang2.photolibrary.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.annotation.StyleRes
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.limuyang2.photolibrary.LPhotoHelper.Companion.EXTRA_COLUMNS_NUMBER
import top.limuyang2.photolibrary.LPhotoHelper.Companion.EXTRA_IS_SINGLE_CHOOSE
import top.limuyang2.photolibrary.LPhotoHelper.Companion.EXTRA_MAX_CHOOSE_COUNT
import top.limuyang2.photolibrary.LPhotoHelper.Companion.EXTRA_PAUSE_ON_SCROLL
import top.limuyang2.photolibrary.LPhotoHelper.Companion.EXTRA_SELECTED_PHOTOS
import top.limuyang2.photolibrary.LPhotoHelper.Companion.EXTRA_TYPE
import top.limuyang2.photolibrary.R
import top.limuyang2.photolibrary.adapter.LPPGridDivider
import top.limuyang2.photolibrary.adapter.PhotoPickerRecyclerAdapter
import top.limuyang2.photolibrary.databinding.LPpActivityPhotoPickerBinding
import top.limuyang2.photolibrary.engine.LImageEngine
import top.limuyang2.photolibrary.util.*


/**
 *
 * Date 2018/7/31
 * @author limuyang
 */

@Suppress("DEPRECATION")
class LPhotoPickerActivity : LBaseActivity<LPpActivityPhotoPickerBinding>() {

    companion object {
        /**
         * 预览照片的请求码
         */
        private const val RC_PREVIEW_CODE = 2
    }


//    // 获取拍照图片保存目录
//    private val cameraFileDir by lazy { intent.getSerializableExtra(EXTRA_CAMERA_FILE_DIR) as File }

    // 获取图片选择的最大张数
    private val maxChooseCount by lazy { intent.getIntExtra(EXTRA_MAX_CHOOSE_COUNT, 1) }

    //外部传进来的已选中的图片路径集合
    private val selectedPhotos by lazy { intent.getStringArrayListExtra(EXTRA_SELECTED_PHOTOS) }

    private val isSingleChoose by lazy { intent.getBooleanExtra(EXTRA_IS_SINGLE_CHOOSE, false) }

    //列数
    private val columnsNumber by lazy { intent.getIntExtra(EXTRA_COLUMNS_NUMBER, 3) }

    private val showTypeArray by lazy { intent.getStringArrayExtra(EXTRA_TYPE) }

    // item图片之间间隔
    private var picSpacing: Int = 0

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        val width = getScreenWidth(this)
        val imgWidth = ((width - picSpacing * (columnsNumber + 1).toFloat()) / columnsNumber.toFloat()).toInt()
        PhotoPickerRecyclerAdapter(maxChooseCount, imgWidth).apply {
            setSelectedItemsPath(selectedPhotos)
        }
    }


    override fun initBinding(): LPpActivityPhotoPickerBinding {
        return LPpActivityPhotoPickerBinding.inflate(layoutInflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        initAttr()

        viewBinding.photoPickerTitle.text = intent.getStringExtra("bucketName")

        initRecyclerView()
        setBottomBtn()

        viewBinding.applyBtn.isEnabled = selectedPhotos != null && selectedPhotos.isNotEmpty()
    }

    /**
     * 获取设置的控件属性
     */
    private fun initAttr() {
        val typedArray = theme.obtainStyledAttributes(R.styleable.LPPAttr)

        val activityBg = typedArray.getColor(R.styleable.LPPAttr_l_pp_picker_activity_bg, Color.parseColor("#F9F9F9"))
        window.setBackgroundDrawable(ColorDrawable(activityBg))

        val statusBarColor = typedArray.getColor(R.styleable.LPPAttr_l_pp_status_bar_color, resources.getColor(R.color.l_pp_colorPrimaryDark))
        setStatusBarColor(statusBarColor)

        val toolBarHeight = typedArray.getDimensionPixelSize(R.styleable.LPPAttr_l_pp_toolBar_height, dip(56).toInt())
        val l = viewBinding.toolBar.layoutParams
        l.height = toolBarHeight
        viewBinding.toolBar.layoutParams = l

        val backIcon = typedArray.getResourceId(R.styleable.LPPAttr_l_pp_toolBar_backIcon, R.drawable.ic_l_pp_back_android)
        viewBinding.toolBar.setNavigationIcon(backIcon)

        val toolBarBackgroundRes = typedArray.getResourceId(R.styleable.LPPAttr_l_pp_toolBar_background, 0)
        val toolBarBackgroundColor = typedArray.getColor(R.styleable.LPPAttr_l_pp_toolBar_background, resources.getColor(R.color.l_pp_colorPrimary))

        if (toolBarBackgroundRes != 0) {
            viewBinding.toolBar.setBackgroundResource(toolBarBackgroundRes)
        } else {
            viewBinding.toolBar.setBackgroundColor(toolBarBackgroundColor)
        }

        val bottomBarBgColor = typedArray.getColor(R.styleable.LPPAttr_l_pp_picker_bottomBar_background, Color.parseColor("#D8FFFFFF"))
        viewBinding.topBlurView.setOverlayColor(bottomBarBgColor)

        val bottomBarHeight = typedArray.getDimensionPixelSize(R.styleable.LPPAttr_l_pp_bottomBar_height, dip(50).toInt())
        val newBl = viewBinding.bottomLayout.layoutParams
        newBl.height = bottomBarHeight
        viewBinding.bottomLayout.requestLayout()

        val bottomBarEnableTextColor = typedArray.getColor(R.styleable.LPPAttr_l_pp_picker_bottomBar_enabled_text_color, Color.parseColor("#333333"))
        val bottomBarUnEnableTextColor = typedArray.getColor(R.styleable.LPPAttr_l_pp_picker_bottomBar_unEnabled_text_color, Color.GRAY)
        val colors = intArrayOf(bottomBarEnableTextColor, bottomBarUnEnableTextColor)
        val states = arrayOfNulls<IntArray>(2)
        states[0] = intArrayOf(android.R.attr.state_enabled)
        states[1] = intArrayOf(android.R.attr.state_window_focused)
        val colorList = ColorStateList(states, colors)
        viewBinding.previewBtn.setTextColor(colorList)
        viewBinding.applyBtn.setTextColor(colorList)

        val titleColor = typedArray.getColor(R.styleable.LPPAttr_l_pp_toolBar_title_color, Color.WHITE)
        val titleSize = typedArray.getDimension(R.styleable.LPPAttr_l_pp_toolBar_title_size, dip(16))
        viewBinding.photoPickerTitle.setTextColor(titleColor)
        viewBinding.photoPickerTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize)

        picSpacing = typedArray.getDimensionPixelOffset(R.styleable.LPPAttr_l_pp_picker_pic_spacing, dip(4).toInt())

        typedArray.recycle()
    }

    private fun initRecyclerView() {
        viewBinding.pickerRecycler.apply {
            layoutManager = GridLayoutManager(this@LPhotoPickerActivity, columnsNumber)
            adapter = this@LPhotoPickerActivity.adapter
            if (isSingleChoose) {
                addItemDecoration(LPPGridDivider(picSpacing, columnsNumber))
                viewBinding.bottomLayout.visibility = View.GONE
            } else {
                addItemDecoration(LPPGridDivider(picSpacing, columnsNumber, viewBinding.bottomLayout.layoutParams.height))
            }

            if (intent.getBooleanExtra(EXTRA_PAUSE_ON_SCROLL, false)) {
                addOnScrollListener(LPPOnScrollListener())
            }
        }
    }

    override fun initListener() {
        viewBinding.toolBar.setNavigationOnClickListener { finishWithCancel() }

        viewBinding.previewBtn.setOnClickListener {
            gotoPreview()
        }

        viewBinding.applyBtn.setOnClickListener(object : OnNoDoubleClickListener() {
            override fun onNoDoubleClick(v: View) {
                finishWithSelectedPhotos(adapter.getSelectedItems())
            }
        })

        adapter.onPhotoItemClick = { view, path, _ ->
            if (isSingleChoose) {
                val list = ArrayList<String>().apply { add(path) }
                finishWithSelectedPhotos(list)
            } else {
                adapter.setChooseItem(path, view.findViewById(R.id.checkView))
                setBottomBtn()
            }
        }

    }

    override fun initData() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                findPhoto(this@LPhotoPickerActivity, intent.getLongExtra("bucketId", -1L), showTypeArray)
            }

            adapter.setData(list)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun setBottomBtn() {
        if (adapter.hasSelected()) {
            viewBinding.applyBtn.isEnabled = true
            viewBinding.applyBtn.text = "${getString(R.string.l_pp_apply)}(${adapter.getSelectedItemSize()}/$maxChooseCount)"

            viewBinding.previewBtn.isEnabled = true
        } else {
            viewBinding.applyBtn.isEnabled = false
            viewBinding.applyBtn.text = getString(R.string.l_pp_apply)

            viewBinding.previewBtn.isEnabled = false
        }
    }

    /**
     * 返回已选中的图片集合
     *
     * @param selectedPhotos
     */
    private fun finishWithSelectedPhotos(selectedPhotos: ArrayList<String>) {
        val intent = Intent()
        intent.putStringArrayListExtra(EXTRA_SELECTED_PHOTOS, selectedPhotos)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun finishWithCancel() {
        val intent = Intent()
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }

    private fun gotoPreview() {
        val intent = LPhotoPickerPreviewActivity.IntentBuilder(this)
                .maxChooseCount(maxChooseCount)
                .selectedPhotos(adapter.getSelectedItems())
                .theme(intentTheme)
                .isFromTakePhoto(false)
                .build()
        startActivityForResult(intent, RC_PREVIEW_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_PREVIEW_CODE -> {
                when (resultCode) {
                    Activity.RESULT_CANCELED -> {
                        data?.let {
                            adapter.setSelectedItemsPath(LPhotoPickerPreviewActivity.getSelectedPhotos(it))
                            setBottomBtn()
                        }
                    }

                    Activity.RESULT_OK -> {
                        data?.let {
                            finishWithSelectedPhotos(LPhotoPickerPreviewActivity.getSelectedPhotos(it))
                        }
                    }
                }
            }
        }
    }

    /**
     * recyclerView 滑动监听，滑动时暂停加载图片
     * @property context Context
     * @constructor
     */
    private class LPPOnScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                ImageEngineUtils.engine.resume(recyclerView.context)
            } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                ImageEngineUtils.engine.pause(recyclerView.context)
            }
        }
    }
}