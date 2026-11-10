package com.mcbjk.aidialog.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：设置界面的滚动算术。
 * <p>
 * 这里守的是一个真实出现过的 bug——maxScroll 曾经写成
 * {@code contentHeight - contentTop - (contentBottom - contentTop)}，
 * 把 contentTop 减了两次，导致可滚动距离凭空少掉 contentTop 那么多。
 * 后果是页面下半部分永远滚不出来，用户看到的就是「设置界面显示不全」。
 * 这个错误不抛异常，只有断言能拦住。
 */
class ScrollMathTest {

	/** 真实布局参数：top=22, TAB_HEIGHT=20, gap=6, 底部留白 28。 */
	private static final int SCREEN_TOP = 22;
	private static final int TAB_HEIGHT = 20;
	private static final int GAP = 6;
	private static final int BOTTOM_MARGIN = 28;

	private static int view(int screenHeight) {
		return ScrollMath.viewHeight(SCREEN_TOP, TAB_HEIGHT, GAP, screenHeight, BOTTOM_MARGIN);
	}

	@Test
	@DisplayName("可视高度按 top + 页签 + 间距 到 屏幕底部留白 计算")
	void viewHeight() {
		// contentTop = 22 + 20 + 6 = 48
		assertEquals(48, ScrollMath.contentTop(SCREEN_TOP, TAB_HEIGHT, GAP));
		// 240 - 28 = 212
		assertEquals(212, ScrollMath.contentBottom(240, BOTTOM_MARGIN));
		// 212 - 48 = 164
		assertEquals(164, view(240));
	}

	@Test
	@DisplayName("人设页在 427x240 下必须能滚 50px（回归用例）")
	void personaTabScrollsFully() {
		// 自检实测值：人设页内容高 214，可视区 164
		int contentHeight = 214;
		int viewHeight = view(240);
		assertEquals(164, viewHeight);
		// 修复前这里会得到 2（因为多减了一个 contentTop=48），底部内容滚不出来
		assertEquals(50, ScrollMath.maxScroll(contentHeight, viewHeight));
	}

	@Test
	@DisplayName("厂商页在 427x240 下必须能滚 227px（回归用例）")
	void providerTabScrollsFully() {
		// 自检实测值：厂商页内容高 391，可视区 164
		assertEquals(227, ScrollMath.maxScroll(391, view(240)));
	}

	@Test
	@DisplayName("绝不允许少算 48px：maxScroll 不可能等于 内容高-可视高-48")
	void neverSubtractsContentTopTwice() {
		for (int contentHeight : new int[]{214, 391, 265, 500}) {
			int v = view(240);
			int correct = contentHeight - v;
			int buggy = contentHeight - 48 - v;
			assertEquals(correct, ScrollMath.maxScroll(contentHeight, v),
					"内容高 " + contentHeight + " 时算错了（疑似又减了一次 contentTop）");
			if (buggy > 0) {
				assertTrue(ScrollMath.maxScroll(contentHeight, v) != buggy,
						"内容高 " + contentHeight + " 命中了多减 contentTop 的错误算法");
			}
		}
	}

	@Test
	@DisplayName("内容装得下时可滚动距离为 0，不会出现负值")
	void noScrollWhenContentFits() {
		assertEquals(0, ScrollMath.maxScroll(100, 164));
		assertEquals(0, ScrollMath.maxScroll(164, 164));
		// 内容比可视区还小时也必须是 0 而不是负数
		assertEquals(0, ScrollMath.maxScroll(10, 164));
	}

	@Test
	@DisplayName("滚到边界后夹紧")
	void clampScroll() {
		assertEquals(0, ScrollMath.clampScroll(-50, 214, 164));
		assertEquals(50, ScrollMath.clampScroll(999, 214, 164));
		assertEquals(25, ScrollMath.clampScroll(25, 214, 164));
		// 内容装得下时任何滚动值都归零
		assertEquals(0, ScrollMath.clampScroll(30, 100, 164));
	}

	@Test
	@DisplayName("窗口越高，可滚动距离越小，直到不需要滚动")
	void tallerWindowScrollsLess() {
		int contentHeight = 391;
		int previous = Integer.MAX_VALUE;
		for (int h : new int[]{240, 320, 400, 440, 480, 600}) {
			int max = ScrollMath.maxScroll(contentHeight, view(h));
			assertTrue(max <= previous, "窗口变高后 maxScroll 反而变大了：h=" + h);
			assertTrue(max >= 0);
			previous = max;
		}
		// 高度足够时完全不需要滚动
		assertEquals(0, ScrollMath.maxScroll(contentHeight, view(600)));
	}
}
