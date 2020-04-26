package top.limuyang2.photolibrary.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.annotation.StyleRes
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import androidx.viewpager.widget.ViewPager
import top.limuyang2.photolibrary.R
import top.limuyang2.photolibrary.adapter.LPreviewPagerAdapter
import top.limuyang2.photolibrary.databinding.LPpActivityPhotoPickerPreviewBinding
import top.limuyang2.photolibrary.util.dp2px
import top.limuyang2.photolibrary.util.statusBarHeight
import top.limuyang2.photolibrary.util.transparentStatusBar

@SuppressLint("SetTextI18n")
class LPhotoPickerPreviewActivity : LBaseActivity<LPpActivityPhotoPickerPreviewBinding>() {

    private val nowSelectedPhotos = ArrayList<String>()

    private val intentMaxChooseCount by lazy { intent.getIntExtra(EXTRA_MAX_CHOOSE_COUNT, 1) }

    private val intentSelectedPhotos by lazy { intent.getStringArrayListExtra(EXTRA_SELECTED_PHOTOS) }

    private val viewPageAdapter by lazy { LPreviewPagerAdapter(supportFragmentManager, intentSelectedPhotos) }

    override fun initBinding(): LPpActivityPhotoPickerPreviewBinding {
        return LPpActivityPhotoPickerPreviewBinding.inflate(layoutInflater)
    }

    override fun getThemeId(): Int = intent.getIntExtra(EXTRA_THEME, R.style.LPhotoTheme)

    override fun initView(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.l_pp_photo_preview_bg)))
        initAttr()
        setStatusBar()
        viewBinding.checkBox.setChecked(checked = true, animate = false)
        viewBinding.previewTitleTv.text = "1/${intentSelectedPhotos.size}"
        viewBinding.viewPage.adapter = viewPageAdapter
    }

    var currentPath = ""

    override fun initListener() {
        viewBinding.toolBar.setNavigationOnClickListener { onBackPressed() }
        viewBinding.viewPage.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                viewBinding.previewTitleTv.text = "${position + 1}/${intentSelectedPhotos.size}"
                currentPath = intentSelectedPhotos[position]
                viewBinding.checkBox.setChecked(nowSelectedPhotos.contains(intentSelectedPhotos[position]), false)
            }
        })

        viewBinding.checkBox.setOnClickListener {
            if (!viewBinding.checkBox.isChecked) {
                viewBinding.checkBox.setChecked(checked = true, animate = true)
                nowSelectedPhotos.add(currentPath)
            } else {
                viewBinding.checkBox.setChecked(checked = false, animate = true)
                nowSelectedPhotos.remove(currentPath)
            }
            viewBinding.applyBtn.isEnabled = nowSelectedPhotos.isNotEmpty()
        }

        viewBinding.applyBtn.setOnClickListener {
            val intent = Intent()
            intent.putStringArrayListExtra(EXTRA_SELECTED_PHOTOS, nowSelectedPhotos)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun initData() {
        nowSelectedPhotos.addAll(intentSelectedPhotos)
        currentPath = intentSelectedPhotos[0]
    }

    private fun initAttr() {
        val typedArray = theme.obtainStyledAttributes(R.styleable.LPPAttr)

        val toolBarHeight = typedArray.getDimensionPixelSize(R.styleable.LPPAttr_l_pp_toolBar_height, dp2px(this, 56f).toInt())
        val l = viewBinding.toolBar.layoutParams
        l.height = toolBarHeight
        viewBinding.toolBar.layoutParams = l

        val backIcon = typedArray.getResourceId(R.styleable.LPPAttr_l_pp_toolBar_backIcon, R.drawable.ic_l_pp_back_android)
        viewBinding.toolBar.setNavigationIcon(backIcon)

        val titleSize = typedArray.getDimension(R.styleable.LPPAttr_l_pp_toolBar_title_size, dp2px(this, 16f))
        viewBinding.previewTitleTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize)

        val bottomBarHeight = typedArray.getDimensionPixelSize(R.styleable.LPPAttr_l_pp_bottomBar_height, dp2px(this, 50f).toInt())
        val newBl = viewBinding.bottomLayout.layoutParams
        newBl.height = bottomBarHeight
        viewBinding.bottomLayout.layoutParams = newBl

        val colors = intArrayOf(Color.WHITE, Color.GRAY)
        val states = arrayOfNulls<IntArray>(2)
        states[0] = intArrayOf(android.R.attr.state_enabled)
        states[1] = intArrayOf(android.R.attr.state_window_focused)
        val colorList = ColorStateList(states, colors)
        viewBinding.applyBtn.setTextColor(colorList)

        typedArray.recycle()
    }

    private fun setStatusBar() {
        //5.0以上去除半透明遮罩，全透明
        transparentStatusBar()

        //获取状态栏高度,设置顶部layout高度
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val allHeight = statusBarHeight + viewBinding.toolBar.layoutParams.height
            val newLayout = viewBinding.topBlurView.layoutParams
            newLayout.height = allHeight
            viewBinding.topBlurView.requestLayout()
        }
    }

    override fun onBackPressed() {
        val intent = Intent()
        intent.putStringArrayListExtra(EXTRA_SELECTED_PHOTOS, nowSelectedPhotos)
//        intent.putExtra(EXTRA_IS_FROM_TAKE_PHOTO, mIsFromTakePhoto)
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }

    private var mIsHidden = false

    fun changeToolBar() {
        if (mIsHidden) {
            showTitleBarAndChooseBar()
        } else {
            hiddenToolBarAndChooseBar()
        }
    }

    private fun showTitleBarAndChooseBar() {
        ViewCompat.animate(viewBinding.toolBarLayout).translationY(0f).setInterpolator(DecelerateInterpolator(2f)).setListener(object : ViewPropertyAnimatorListenerAdapter() {
            override fun onAnimationEnd(view: View?) {
                mIsHidden = false
            }
        }).setDuration(DURATION_TIME).start()
        viewBinding.toolBarLayout.visibility = View.VISIBLE

        ViewCompat.animate(viewBinding.bottomLayout).translationY(0f).setInterpolator(DecelerateInterpolator(2f)).setListener(object : ViewPropertyAnimatorListenerAdapter() {
            override fun onAnimationEnd(view: View?) {
                mIsHidden = false
            }
        }).setDuration(DURATION_TIME).start()
        viewBinding.bottomLayout.visibility = View.VISIBLE
    }

    private fun hiddenToolBarAndChooseBar() {
        ViewCompat.animate(viewBinding.toolBarLayout).translationY((-viewBinding.toolBarLayout.height).toFloat()).setInterpolator(DecelerateInterpolator(2f)).setListener(object : ViewPropertyAnimatorListenerAdapter() {
            override fun onAnimationEnd(view: View?) {
                mIsHidden = true
                viewBinding.toolBarLayout.visibility = View.GONE
            }
        }).setDuration(DURATION_TIME).start()

        ViewCompat.animate(viewBinding.bottomLayout).translationY((viewBinding.bottomLayout.height).toFloat()).setInterpolator(DecelerateInterpolator(2f)).setListener(object : ViewPropertyAnimatorListenerAdapter() {
            override fun onAnimationEnd(view: View?) {
                mIsHidden = true
                viewBinding.bottomLayout.visibility = View.GONE
            }
        }).setDuration(DURATION_TIME).start()

    }

    class IntentBuilder(context: Context) {
        private val mIntent: Intent = Intent(context, LPhotoPickerPreviewActivity::class.java)

        /**
         * 图片选择张数的最大值
         */
        fun maxChooseCount(maxChooseCount: Int): IntentBuilder {
            mIntent.putExtra(EXTRA_MAX_CHOOSE_COUNT, maxChooseCount)
            return this
        }

        /**
         * 当前已选中的图片路径集合
         */
        fun selectedPhotos(selectedPhotos: ArrayList<String>): IntentBuilder {
            mIntent.putStringArrayListExtra(EXTRA_SELECTED_PHOTOS, selectedPhotos)
            return this
        }

        /**
         * 是否是拍完照后跳转过来
         */
        fun isFromTakePhoto(isFromTakePhoto: Boolean): IntentBuilder {
            mIntent.putExtra(EXTRA_IS_FROM_TAKE_PHOTO, isFromTakePhoto)
            return this
        }

        /**
         * 设置主题
         */
        fun theme(@StyleRes style: Int): IntentBuilder {
            mIntent.putExtra(EXTRA_THEME, style)
            return this
        }

        fun build(): Intent {
            return mIntent
        }
    }

    companion object {
        private const val DURATION_TIME = 600L

        //        private const val EXTRA_PREVIEW_PHOTOS = "EXTRA_PREVIEW_PHOTOS"
        private const val EXTRA_SELECTED_PHOTOS = "EXTRA_SELECTED_PHOTOS"
        private const val EXTRA_MAX_CHOOSE_COUNT = "EXTRA_MAX_CHOOSE_COUNT"
        private const val EXTRA_THEME = "EXTRA_THEME"
        private const val EXTRA_IS_FROM_TAKE_PHOTO = "EXTRA_IS_FROM_TAKE_PHOTO"

        /**
         * 获取已选择的图片集合
         *
         * @param intent
         * @return
         */
        fun getSelectedPhotos(intent: Intent): ArrayList<String> {
            return intent.getStringArrayListExtra(EXTRA_SELECTED_PHOTOS)
        }
    }
}