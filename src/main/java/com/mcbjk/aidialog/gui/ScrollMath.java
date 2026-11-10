package com.mcbjk.aidialog.gui;

/**
 * 可滚动内容区的算术。
 * 单独抽出来是因为这段计算很容易出「坐标基准混用」的错：
 * 控件的 baseY 是相对内容区顶部的偏移，而可视高度是屏幕坐标之差，
 * 两者不能相减——早先版本就因为在同一条式子里混用而少算了 contentTop 那么多，
 * 结果页面底部的内容永远滚不出来（表现为「设置界面显示不全」）。
 * 这类错误不会抛异常，只能靠断言守住。
 */
public final class ScrollMath {
	private ScrollMath() {
	}

	/**
	 * 还能往下滚多少像素。
	 *
	 * @param contentHeight 内容总高度（baseY + 控件高度，相对内容区顶部）
	 * @param viewHeight    可视区高度（contentBottom - contentTop）
	 * @return 可滚动的最大像素，至少为 0
	 */
	public static int maxScroll(int contentHeight, int viewHeight) {
		return Math.max(0, contentHeight - viewHeight);
	}

	/**
	 * 把一个滚动值夹到合法范围。
	 */
	public static int clampScroll(int value, int contentHeight, int viewHeight) {
		return Math.max(0, Math.min(value, maxScroll(contentHeight, viewHeight)));
	}

	/**
	 * 内容区顶部在屏幕上的 Y 坐标。
	 * 内容区在页签栏下方，底部按钮上方。
	 */
	public static int contentTop(int screenTop, int tabHeight, int gap) {
		return screenTop + tabHeight + gap;
	}

	/** 内容区底部在屏幕上的 Y 坐标（给底部按钮留出空间）。 */
	public static int contentBottom(int screenHeight, int bottomMargin) {
		return screenHeight - bottomMargin;
	}

	/** 可视区高度。 */
	public static int viewHeight(int screenTop, int tabHeight, int gap, int screenHeight, int bottomMargin) {
		return contentBottom(screenHeight, bottomMargin) - contentTop(screenTop, tabHeight, gap);
	}
}
