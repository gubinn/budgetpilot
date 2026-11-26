<template>
  <div class="an-page" :class="{ mobile }">
    <div class="an-head">
      <div class="an-title">
        <h2>年度复盘</h2>
        <p v-if="data">{{ periodLabel }}</p>
        <p v-else>加载中…</p>
      </div>
      <div class="an-filters">
        <n-select
          v-model:value="year"
          :options="yearOptions"
          :size="mobile ? 'large' : 'small'"
          class="an-filter-year"
          style="width: 112px"
          @update:value="reload"
        />
        <n-radio-group
          v-model:value="level"
          :size="mobile ? 'medium' : 'small'"
          class="an-filter-level"
          @update:value="reload"
        >
          <n-radio-button :value="1">一级</n-radio-button>
          <n-radio-button :value="2">二级</n-radio-button>
        </n-radio-group>
        <n-button size="tiny" quaternary :loading="loading" @click="reload">刷新</n-button>
      </div>
    </div>

    <n-alert v-if="dataStates.partialYear" type="info" :bordered="false" class="an-alert">
      本年还没过完，数据截至今天；月均按已过月份计算。
    </n-alert>
    <n-alert v-if="data && dataStates.monthsWithData > 0 && dataStates.monthsWithData < 3" type="warning" :bordered="false" class="an-alert">
      本年度只有 {{ dataStates.monthsWithData }} 个月有流水，年度结论仅供参考。
    </n-alert>
    <n-alert v-if="dataStates.compareInsufficient" type="warning" :bordered="false" class="an-alert">
      去年同期数据不足，同比仅供参考。
    </n-alert>
    <n-alert v-if="data && !dataStates.compareAvailable && !dataStates.noData" type="info" :bordered="false" class="an-alert">
      去年没有可比数据，已隐藏同比。
    </n-alert>
    <n-alert v-if="dataStates.noData" type="warning" :bordered="false" class="an-alert">
      本年度暂无流水。换一个年份，或确认交易是否已入账、已复核。
    </n-alert>

    <n-spin :show="loading">
      <div v-if="data" class="an-body" :class="{ dim: loading }">
        <section class="an-hero-grid">
          <article class="an-card an-hero">
            <div class="an-eyebrow">年度成绩单 · {{ periodLabel }}</div>
            <div class="an-hero-main">
              <div>
                <div class="an-figure" :class="{ neg: Number(kpis.net) < 0 }">
                  {{ signedMoney(kpis.net) }}
                </div>
                <div class="an-delta-line">
                  <span class="an-chip" :class="savingsTone">储蓄率 {{ Number(kpis.savingsRate || 0).toFixed(1) }}%</span>
                  <span v-if="savingsDelta.text !== '—'" class="an-chip" :class="savingsDelta.tone">
                    较去年同期 {{ savingsDelta.text }}
                  </span>
                  <span v-else class="an-muted">{{ compareNoteText }}</span>
                </div>
              </div>
              <div class="an-hero-side">
                <div><span>全年收入</span><strong>{{ signedMoney(kpis.income) }}</strong></div>
                <div><span>全年支出</span><strong>{{ signedMoney(kpis.expense) }}</strong></div>
              </div>
            </div>
            <div class="an-segbar" role="img" aria-label="支出结构">
              <button
                v-for="s in scopeList.filter(x => Number(x.amount) > 0.004)"
                :key="s.scope"
                type="button"
                :style="{ flex: Math.max(Number(s.amount), 0.01), background: scopeColor(s.scope) }"
                :title="`${scopeShort(s.scope)} ¥${money(s.amount)} · ${s.share}%`"
              >
                <span v-if="Number(s.share) >= 12">{{ s.share }}%</span>
              </button>
            </div>
            <div class="an-seglegend">
              <div v-for="s in scopeList" :key="s.scope" class="an-leg">
                <i :style="{ background: scopeColor(s.scope) }"></i>
                <span>{{ scopeShort(s.scope) }}</span>
                <b>{{ signedMoney(s.amount) }}</b>
                <span class="an-muted">{{ s.share }}%</span>
              </div>
              <div class="an-seg-total">合计 <strong>{{ signedMoney(kpis.expense) }}</strong></div>
            </div>
          </article>

          <div class="an-stats">
            <div v-for="s in stats" :key="s.label" class="an-stat">
              <div>
                <div class="an-stat-label">{{ s.label }}</div>
                <div class="an-stat-value">{{ s.value }}</div>
              </div>
              <div class="an-stat-foot">
                <span v-if="s.delta && s.delta.text !== '—'" class="an-chip" :class="s.delta.tone">{{ s.delta.text }}</span>
                <span class="an-muted">{{ s.note }}</span>
              </div>
            </div>
          </div>
        </section>

        <section class="an-grid an-grid-8-4">
          <article class="an-card">
            <div class="an-card-head">
              <div>
                <h3>收支节奏</h3>
                <p class="an-cap">12 个月 · 收入/支出柱 + 结余线</p>
              </div>
              <button class="an-link" @click="showMonthlyTable = !showMonthlyTable">
                {{ showMonthlyTable ? '收起数据表' : '数据表' }}
              </button>
            </div>
            <div ref="monthlyEl" class="an-chart" :style="{ height: mobile ? '230px' : '300px' }"></div>
            <div class="an-month-foot">
              <span v-if="bestMonth">结余最高 <b>{{ bestMonth.label }}</b> · {{ signedMoney(bestMonth.net) }}</span>
              <span v-if="highestExpenseMonth">支出最高 <b>{{ highestExpenseMonth.label }}</b> · {{ signedMoney(highestExpenseMonth.expense) }}</span>
              <span>月均结余 <b>{{ signedMoney(kpis.avgMonthlyNet) }}</b></span>
            </div>
            <table v-if="showMonthlyTable" class="an-table">
              <thead>
                <tr><th>月份</th><th class="n">收入</th><th class="n">支出</th><th class="n">结余</th><th class="n">储蓄率</th><th class="n">累计结余</th></tr>
              </thead>
              <tbody>
                <tr v-for="m in monthly" :key="m.month" :class="{ future: m.future }">
                  <td>{{ m.label }}</td>
                  <td class="n">{{ m.future ? '—' : '¥' + money(m.income) }}</td>
                  <td class="n">{{ m.future ? '—' : signedMoney(m.expense) }}</td>
                  <td class="n">{{ m.future ? '—' : signedMoney(m.net) }}</td>
                  <td class="n">{{ m.future ? '—' : Number(m.savingsRate || 0).toFixed(1) + '%' }}</td>
                  <td class="n">{{ m.future ? '—' : signedMoney(m.cumulativeNet) }}</td>
                </tr>
              </tbody>
            </table>
          </article>

          <article class="an-card">
            <div class="an-card-head">
              <div>
                <h3>财务健康</h3>
                <p class="an-cap">四个年度体检指标</p>
              </div>
            </div>
            <div class="an-health">
              <div v-for="h in health" :key="h.label" class="an-health-row">
                <div class="an-health-top">
                  <span>{{ h.label }}</span>
                  <b :class="h.tone">{{ h.value }}</b>
                </div>
                <div class="an-health-track"><span :class="h.tone" :style="{ width: h.pct + '%' }"></span></div>
                <div class="an-health-note">{{ h.note }}</div>
              </div>
            </div>
          </article>
        </section>

        <section class="an-grid an-grid-7-5">
          <article class="an-card">
            <div class="an-card-head">
              <div>
                <h3>净资产轨迹</h3>
                <p class="an-cap">{{ netWorth.startLabel || '上年 12 月' }} → {{ netWorth.endLabel || '期末' }} · 月末快照</p>
              </div>
              <button v-if="netWorth.netWorthAvailable" class="an-link" @click="showWorthTable = !showWorthTable">
                {{ showWorthTable ? '收起数据表' : '数据表' }}
              </button>
            </div>
            <template v-if="netWorth.netWorthAvailable">
              <div class="an-worth-top">
                <div><span>期初</span><strong>{{ signedMoney(netWorth.startNetWorth) }}</strong></div>
                <div><span>期末</span><strong>{{ signedMoney(netWorth.endNetWorth) }}</strong></div>
                <div>
                  <span>变化</span>
                  <strong :class="Number(netWorth.change) >= 0 ? 'good' : 'bad'">
                    {{ signedMoney(netWorth.change) }}
                    <small v-if="netWorth.changePct != null"> · {{ signedPct(netWorth.changePct) }}</small>
                  </strong>
                </div>
              </div>
              <div ref="worthEl" class="an-chart" :style="{ height: mobile ? '190px' : '230px' }"></div>
              <div class="an-worth-stats">
                <div><span>资产</span><b>¥{{ money(netWorth.assets) }}</b></div>
                <div><span>负债</span><b class="bad">¥{{ money(netWorth.liabilities) }}</b></div>
                <div><span>可动用</span><b>¥{{ money(netWorth.liquidAssets) }}</b></div>
                <div>
                  <span>应急金</span>
                  <b>{{ netWorth.emergencyFundMonths != null ? Number(netWorth.emergencyFundMonths).toFixed(1) + ' 个月' : '—' }}</b>
                </div>
              </div>
              <div v-if="netWorth.groups.length" class="an-worth-groups">
                <span v-for="g in netWorth.groups" :key="g.type">
                  {{ g.type }}
                  <b :class="{ bad: Number(g.balance) < 0 }">{{ signedMoney(g.balance) }}</b>
                </span>
              </div>
              <p v-if="netWorth.otherChange != null && Math.abs(Number(netWorth.otherChange)) > 1" class="an-note">
                全年结余 {{ signedMoney(netWorth.annualNet) }}，净资产变化 {{ signedMoney(netWorth.change) }}，
                差额 {{ signedMoney(netWorth.otherChange) }}（投资增值、往来款、余额调整或未记账等）。
              </p>
              <table v-if="showWorthTable" class="an-table">
                <thead><tr><th>月份</th><th class="n">净资产</th><th class="n">环比</th></tr></thead>
                <tbody>
                  <tr v-for="(n, i) in nwRows" :key="n.month">
                    <td>{{ n.month }}</td>
                    <td class="n">{{ signedMoney(n.netWorth) }}</td>
                    <td class="n" :class="n.change > 0 ? 'good' : n.change < 0 ? 'bad' : ''">
                      {{ n.change == null ? '—' : (n.change >= 0 ? '+' : '') + signedMoney(n.change) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </template>
            <n-empty v-else description="暂无净资产快照" />
          </article>

          <div class="an-side-stack">
            <article class="an-card">
              <div class="an-card-head">
                <div>
                  <h3>年度预算执行</h3>
                  <p class="an-cap">固定 + 年度专项 · 全年口径</p>
                </div>
              </div>
              <template v-if="annualBudget.exists">
                <div class="an-budget-hero">
                  <div>
                    <div class="an-budget-value">¥{{ money(annualBudget.spent) }}<small> / ¥{{ money(annualBudget.total) }}</small></div>
                    <div class="an-cap">
                      {{ Number(annualBudget.remaining) < 0 ? '超支' : '剩余' }}
                      <b :class="Number(annualBudget.remaining) < 0 ? 'bad' : 'good'">¥{{ money(Math.abs(Number(annualBudget.remaining))) }}</b>
                    </div>
                  </div>
                  <span class="an-pct" :class="annualTone">{{ annualBudget.pct }}%</span>
                </div>
                <div class="an-meter">
                  <span :class="annualTone" :style="{ width: Math.min(Number(annualBudget.pct), 100) + '%' }"></span>
                  <i class="an-time-mark" :style="{ left: Math.min(Number(annualBudget.timePct), 100) + '%' }"></i>
                </div>
                <div class="an-cap an-meter-cap">
                  <span>花费进度 {{ annualBudget.pct }}%</span>
                  <span>时间进度 {{ annualBudget.timePct }}%</span>
                </div>
                <div class="an-budget-list">
                  <div v-for="b in annualBudget.items.slice(0, 5)" :key="b.categoryId" class="an-brow">
                    <span class="an-brow-name">{{ b.name }}</span>
                    <span class="an-brow-track">
                      <span :class="Number(b.pct) >= 100 ? 'done' : Number(b.pct) >= 90 ? 'warn' : ''" :style="{ width: Math.min(Number(b.pct), 100) + '%' }"></span>
                    </span>
                    <span class="an-brow-val">{{ Number(b.pct).toFixed(0) }}%</span>
                  </div>
                </div>
                <div v-if="annualBudget.items.length > 5" class="an-cap an-more">
                  其余 {{ annualBudget.items.length - 5 }} 项：{{ annualBudget.items.slice(5).map(b => `${b.name} ${Number(b.pct).toFixed(0)}%`).join(' · ') }}
                </div>
              </template>
              <n-empty v-else description="未设置年度预算">
                <template #extra>
                  <n-button v-if="!auth.isReadOnly" size="small" @click="router.push('/budget')">去设置</n-button>
                </template>
              </n-empty>
            </article>

            <article class="an-card">
              <div class="an-card-head">
                <div>
                  <h3>月度预算纪律</h3>
                  <p class="an-cap">已设预算的月份 · 不含未来月份</p>
                </div>
              </div>
              <template v-if="monthlyBudget.exists">
                <div class="an-mini-stats">
                  <div><span>设预算</span><b>{{ monthlyBudget.monthsWithBudget }} 个月</b></div>
                  <div><span>超支</span><b :class="monthlyBudget.monthsOver ? 'bad' : 'good'">{{ monthlyBudget.monthsOver }} 个月</b></div>
                  <div><span>执行率</span><b>{{ monthlyBudget.pct }}%</b></div>
                </div>
                <div class="an-cap">预算合计 ¥{{ money(monthlyBudget.budgetTotal) }} · 实际 ¥{{ money(monthlyBudget.spentTotal) }}</div>
                <div v-if="monthlyBudget.items.some(m => m.over)" class="an-over-list">
                  <span v-for="m in monthlyBudget.items.filter(x => x.over)" :key="m.month" class="an-over-chip">{{ m.month }} 超支</span>
                </div>
              </template>
              <n-empty v-else description="本年度未设置月度预算" />
            </article>
          </div>
        </section>

        <n-button v-if="mobile && !showMore" block secondary style="margin-bottom: 16px" @click="toggleMore">
          展开分类、商户与年度之最
        </n-button>

        <section v-if="!mobile || showMore" class="an-grid an-grid-7-5">
          <article class="an-card">
            <div class="an-card-head">
              <div>
                <h3>分类年度排行</h3>
                <p class="an-cap">按金额降序 · 点任意一行查看明细</p>
              </div>
              <button class="an-link" @click="showRankTable = !showRankTable">
                {{ showRankTable ? '收起数据表' : '数据表' }}
              </button>
            </div>
            <div class="an-rank-head">
              <span>#</span><span></span><span>分类</span><span>分布</span>
              <span class="n">金额</span><span class="n">同比</span>
            </div>
            <button v-for="(c, i) in categories" :key="c.categoryId" class="an-rank-row" @click="openCategory(c)">
              <span class="an-rank-no">{{ i + 1 }}</span>
              <i class="an-dot" :style="{ background: c.color || '#98a2b3' }"></i>
              <span class="an-rank-name">
                <span class="nm">{{ c.name }}</span>
                <span class="meta">
                  {{ c.count }} 笔 · {{ c.share }}%<span v-if="c.parentName"> · {{ c.parentName }}</span>
                  · 月均 ¥{{ money(c.avgPerMonth) }}
                </span>
              </span>
              <span class="an-rank-bar"><span :style="{ width: rankWidth(c) }"></span></span>
              <span class="an-rank-amt">¥{{ money(c.amount) }}</span>
              <span class="an-rank-delta" :class="rankDelta(c).tone">{{ rankDelta(c).text }}</span>
            </button>
            <p v-if="!categories.length" class="an-cap" style="padding: 16px 4px">当前范围没有支出</p>
            <table v-if="showRankTable" class="an-table">
              <thead><tr><th>#</th><th>分类</th><th class="n">金额</th><th class="n">占比</th><th class="n">笔数</th><th class="n">同比</th></tr></thead>
              <tbody>
                <tr v-for="(c, i) in categories" :key="c.categoryId">
                  <td>{{ i + 1 }}</td>
                  <td>{{ c.parentName ? `${c.parentName} / ` : '' }}{{ c.name }}</td>
                  <td class="n">¥{{ money(c.amount) }}</td>
                  <td class="n">{{ c.share }}%</td>
                  <td class="n">{{ c.count }}</td>
                  <td class="n">{{ rankDelta(c).text }}</td>
                </tr>
              </tbody>
            </table>
          </article>

          <article class="an-card">
            <div class="an-card-head">
              <div>
                <h3>收入来源</h3>
                <p class="an-cap">按一级收入分类 · 占比与同比</p>
              </div>
            </div>
            <div v-for="c in incomeCategories" :key="c.categoryId || 'none'" class="an-income-row">
              <div class="an-income-main">
                <span class="nm">{{ c.name }}</span>
                <span class="meta">{{ c.count }} 笔</span>
              </div>
              <div class="an-income-bar"><span :style="{ width: incomeWidth(c), background: c.color || '#0ea5a4' }"></span></div>
              <div class="an-income-amt">¥{{ money(c.amount) }}<small>{{ c.share }}%</small></div>
              <span class="an-rank-delta" :class="rankDelta(c, true).tone">{{ rankDelta(c, true).text }}</span>
            </div>
            <n-empty v-if="!incomeCategories.length" description="暂无收入分类数据" />
          </article>
        </section>

        <section v-if="!mobile || showMore" class="an-grid an-grid-7-5">
          <article class="an-card">
            <div class="an-card-head">
              <div>
                <h3>商户排行</h3>
                <p class="an-cap">全年支出前 {{ Math.min(merchants.length, 10) }} 名</p>
              </div>
            </div>
            <div v-for="m in merchants" :key="m.merchantId" class="an-merchant-row">
              <div class="an-merchant-main">
                <div class="an-merchant-name">{{ m.name }}</div>
                <div class="an-merchant-meta">
                  <span class="an-badge">{{ m.categoryName || '未分类' }}</span>
                  <span>{{ m.count }} 笔</span>
                  <span>{{ m.share }}%</span>
                </div>
              </div>
              <div class="an-merchant-bar"><span :style="{ width: merchantWidth(m) }"></span></div>
              <div class="an-merchant-amt">¥{{ money(m.amount) }}</div>
            </div>
            <n-empty v-if="!merchants.length" description="暂无商户数据" />
          </article>

          <article class="an-card">
            <div class="an-card-head">
              <div>
                <h3>年度之最</h3>
                <p class="an-cap">一年里最值得记住的几件事</p>
              </div>
            </div>
            <div class="an-moments">
              <div v-for="m in moments" :key="m.label" class="an-moment">
                <div class="an-moment-label">{{ m.label }}</div>
                <div class="an-moment-value">{{ m.value }}</div>
                <div class="an-moment-note">{{ m.note }}</div>
              </div>
            </div>
            <n-empty v-if="!moments.length" description="暂无年度之最" />
          </article>
        </section>

        <section class="an-insights">
          <div v-for="(x, i) in insights" :key="i" class="an-insight">
            <div class="an-insight-icon" :class="x.tone">
              <svg v-if="x.tone === 'bad'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 9v4M12 17h.01"/><path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z"/></svg>
              <svg v-else-if="x.tone === 'warn'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m4 17 5-5 4 4 7-8"/><path d="M15 8h5v5"/></svg>
              <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 3v18M3 12h18"/></svg>
            </div>
            <div>
              <h4>{{ x.title }}</h4>
              <p>{{ x.text }}</p>
            </div>
          </div>
        </section>

        <p class="an-foot">
          口径：只统计已入账且已复核的流水，退款冲减；固定 / 专项按分类的「预算类型」归集；
          转账、往来款、余额调整不计入收支。净资产为按月快照估算，可能受余额校准影响；
          卡券包是表外备忘，不计入净资产；往来款流水会影响账户余额，但应收/应付本身不单独计入净资产。
          <template v-if="dataStates.multiCurrency"> 本期含外币交易，金额按入账时汇率折算为 CNY。</template>
        </p>
      </div>
      <n-empty v-else-if="!loading" description="暂无年度数据" />
    </n-spin>

    <n-drawer
      v-model:show="drawer.show"
      :placement="mobile ? 'bottom' : 'right'"
      :height="mobile ? '82%' : undefined"
      :width="mobile ? undefined : 440"
    >
      <n-drawer-content :title="drawer.category ? drawer.category.name : '分类明细'" closable>
        <template v-if="drawer.category">
          <div class="an-drawer-total">¥{{ money(drawer.category.amount) }}</div>
          <div class="an-cap">
            占全年 {{ drawer.category.share }}% · {{ drawer.category.count }} 笔
            <span v-if="drawer.category.parentName"> · {{ drawer.category.parentName }}</span>
          </div>

          <div class="an-drawer-sec">
            <h4>交易明细</h4>
            <n-spin :show="drawer.loading">
              <div v-for="t in drawer.items" :key="t.id" class="an-drawer-tx">
                <div>
                  <div class="an-drawer-tx-note">{{ t.note || t.accountName || '—' }}</div>
                  <div class="an-cap">{{ t.transactionDate }}<template v-if="t.accountName"> · {{ t.accountName }}</template></div>
                </div>
                <div class="an-drawer-tx-amt" :class="{ refund: t.type === 4 }">
                  {{ t.type === 4 ? '+' : '-' }}¥{{ money(t.amount) }}
                </div>
              </div>
              <n-empty v-if="!drawer.loading && !drawer.items.length" description="暂无交易" />
            </n-spin>
          </div>
          <div class="an-drawer-note">
            年度分类明细按本年 1 月 1 日到当前区间统计；点开后可核对具体交易。
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { reportApi } from '@/api'
import { useReportRefresh } from '@/composables/useReportRefresh'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  mobile: { type: Boolean, default: false }
})

const route = useRoute()
const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

const currentYear = dayjs().year()
const year = ref(normalizeYear(route.query.year))
const level = ref(Number(route.query.level) === 2 ? 2 : 1)
const data = ref(null)
const loading = ref(false)
const showMore = ref(false)
const showMonthlyTable = ref(false)
const showWorthTable = ref(false)
const showRankTable = ref(false)
const drawer = ref({ show: false, loading: false, category: null, items: [] })

const monthlyEl = ref(null)
const worthEl = ref(null)
let monthlyChart = null
let worthChart = null
let requestSeq = 0

const yearOptions = computed(() => {
  const years = new Set(Array.from({ length: 6 }, (_, i) => currentYear - i))
  // 深链到 6 年以前的年份时，也把它放进下拉框，避免 select 显示空白
  years.add(year.value)
  return [...years]
    .sort((a, b) => b - a)
    .map(y => ({ label: `${y} 年`, value: y }))
})

const kpis = computed(() => data.value?.kpis || {})
const period = computed(() => data.value?.period || {})
const dataStates = computed(() => data.value?.dataStates || {})
const scopeList = computed(() => {
  const map = new Map((data.value?.scopes || []).map(s => [s.scope, s]))
  return ['DAILY', 'FIXED', 'SPECIAL'].map(scope => map.get(scope) || {
    scope, amount: 0, share: 0, count: 0
  })
})
const monthly = computed(() => data.value?.monthly || [])
const categories = computed(() => data.value?.categories || [])
const incomeCategories = computed(() => data.value?.incomeCategories || [])
const merchants = computed(() => data.value?.merchants || [])
const insights = computed(() => data.value?.insights || [])
const netWorth = computed(() => {
  const n = data.value?.netWorth || {}
  return { ...n, trend: n.trend || [], groups: n.groups || [] }
})
const annualBudget = computed(() => {
  const b = data.value?.annualBudget || {}
  return { ...b, items: b.items || [] }
})
const monthlyBudget = computed(() => {
  const b = data.value?.monthlyBudget || {}
  return { ...b, items: b.items || [] }
})
const bestMonth = computed(() => data.value?.bestMonth || null)
const highestExpenseMonth = computed(() => {
  const items = monthly.value.filter(m => !m.future && m.hasData)
  if (!items.length) return null
  return items.reduce((a, b) => Number(b.expense) > Number(a.expense) ? b : a)
})

const periodLabel = computed(() => {
  const p = period.value
  if (!p.year) return ''
  return p.partialYear
    ? `${p.year} 年 · 截至 ${p.monthsCovered} 月`
    : `${p.year} 年全年`
})

const compareNoteText = computed(() => {
  if (dataStates.value.compareAvailable) {
    return period.value.compareSamePeriod ? '较去年同期' : '较去年'
  }
  return '去年无可比数据'
})

const savingsTone = computed(() => {
  const rate = Number(kpis.value.savingsRate || 0)
  if (rate >= 30) return 'good'
  if (rate < 0) return 'bad'
  return rate >= 10 ? 'neutral' : 'warn'
})

const savingsDelta = computed(() => {
  const value = kpis.value.savingsRateChangePp
  if (!dataStates.value.compareAvailable || value == null) return { text: '—', tone: 'flat' }
  const n = Number(value)
  return {
    text: `${n > 0 ? '+' : ''}${n.toFixed(1)}pp`,
    tone: n > 0 ? 'good' : n < 0 ? 'bad' : 'flat'
  }
})

function deltaChip(value, upIsGood) {
  if (!dataStates.value.compareAvailable || value == null) return { text: '—', tone: 'flat' }
  const n = Number(value)
  if (Math.abs(n) < 0.05) return { text: '持平', tone: 'flat' }
  return {
    text: `${n > 0 ? '↑' : '↓'} ${Math.abs(n).toFixed(1)}%`,
    tone: (upIsGood ? n > 0 : n < 0) ? 'good' : 'bad'
  }
}

const stats = computed(() => [
  {
    label: '全年收入',
    value: signedMoney(kpis.value.income),
    delta: deltaChip(kpis.value.incomeChangePct, true),
    note: compareNoteText.value
  },
  {
    label: '全年支出',
    value: signedMoney(kpis.value.expense),
    delta: deltaChip(kpis.value.expenseChangePct, false),
    note: compareNoteText.value
  },
  {
    label: '月均支出',
    value: signedMoney(kpis.value.avgMonthlyExpense),
    delta: null,
    note: period.value.partialYear ? `按 ${period.value.monthsCovered} 个月` : '按 12 个月'
  },
  {
    label: '净资产变化',
    value: netWorth.value.netWorthAvailable ? signedMoney(netWorth.value.change) : '—',
    delta: null,
    note: netWorth.value.netWorthAvailable
      ? `期初 ${signedMoney(netWorth.value.startNetWorth)} → 期末 ${signedMoney(netWorth.value.endNetWorth)}`
      : '暂无快照'
  }
])

const health = computed(() => {
  const rate = Number(kpis.value.savingsRate || 0)
  const emergency = netWorth.value.emergencyFundMonths
  const fixed = Number(scopeAmount('FIXED'))
  const total = Number(kpis.value.expense || 0)
  const fixedShare = total > 0 ? fixed / total * 100 : 0
  const activityDays = Number(kpis.value.activityDays || 0)
  const spendingDays = Number(kpis.value.spendingDays || 0)
  return [
    {
      label: '储蓄率',
      value: `${rate.toFixed(1)}%`,
      tone: savingsTone.value,
      pct: clamp(rate, 0, 100),
      note: '参考线 30%'
    },
    {
      label: '应急金',
      value: emergency != null ? `${Number(emergency).toFixed(1)} 个月` : '—',
      tone: emergency == null ? 'flat'
        : Number(emergency) >= 6 ? 'good' : Number(emergency) >= 3 ? 'warn' : 'bad',
      pct: emergency == null ? 0 : clamp(Number(emergency) / 6 * 100, 0, 100),
      note: `目标 6 个月 · 日常+固定月均 ¥${money(netWorth.value.essentialMonthlyExpense)}`
    },
    {
      label: '固定支出占比',
      value: `${fixedShare.toFixed(1)}%`,
      tone: fixedShare > 50 ? 'warn' : fixedShare > 35 ? 'neutral' : 'good',
      pct: clamp(fixedShare, 0, 100),
      note: '固定支出 ÷ 全部支出'
    },
    {
      label: '有支出天数',
      value: `${spendingDays} 天`,
      tone: 'neutral',
      pct: activityDays > 0 ? clamp(spendingDays / activityDays * 100, 0, 100) : 0,
      note: `有流水 ${activityDays} 天 · 无支出 ${Number(kpis.value.noSpendDays || 0)} 天`
    }
  ]
})

const moments = computed(() => {
  const list = []
  const largest = data.value?.largestExpense
  if (largest) {
    list.push({
      label: '单笔最大支出',
      value: `¥${money(largest.amount)}`,
      note: [largest.date, largest.categoryName, largest.merchantName].filter(Boolean).join(' · ')
    })
  }
  if (bestMonth.value) {
    list.push({
      label: '结余最高月份',
      value: bestMonth.value.label,
      note: signedMoney(bestMonth.value.net)
    })
  }
  if (highestExpenseMonth.value) {
    list.push({
      label: '支出最高月份',
      value: highestExpenseMonth.value.label,
      note: `支出 ¥${money(highestExpenseMonth.value.expense)}`
    })
  }
  if (merchants.value[0]) {
    list.push({
      label: '消费最多商户',
      value: merchants.value[0].name,
      note: `¥${money(merchants.value[0].amount)} · ${merchants.value[0].count} 笔`
    })
  }
  if (categories.value[0]) {
    list.push({
      label: '最大支出分类',
      value: categories.value[0].name,
      note: `¥${money(categories.value[0].amount)} · ${categories.value[0].share}%`
    })
  }
  return list
})

const annualTone = computed(() => {
  const budget = annualBudget.value
  if (!budget.exists || !Number(budget.total)) return 'ok'
  if (Number(budget.pct) > 100) return 'bad'
  return Number(budget.pct) > Number(budget.timePct) + 5 ? 'warn' : 'ok'
})

const nwRows = computed(() => {
  const items = netWorth.value.trend || []
  return items.map((x, i) => ({
    ...x,
    change: i === 0 ? null : Number(x.netWorth) - Number(items[i - 1].netWorth)
  }))
})

const maxRankAmount = computed(() =>
  Math.max(...categories.value.map(c => Number(c.amount)), 1))
const maxIncomeAmount = computed(() =>
  Math.max(...incomeCategories.value.map(c => Number(c.amount)), 1))
const maxMerchantAmount = computed(() =>
  Math.max(...merchants.value.map(m => Number(m.amount)), 1))

const COLORS = { DAILY: '#2a78d6', FIXED: '#eb6834', SPECIAL: '#1baf7a' }
const nf0 = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 })
const nf2 = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function normalizeYear(value) {
  const n = Number(value)
  return Number.isInteger(n) && n >= 2000 && n <= currentYear ? n : currentYear
}

function money(value, dec = 0) {
  const v = Number(value) || 0
  return dec === 2 ? nf2.format(v) : nf0.format(v)
}

function signedMoney(value, dec = 0) {
  const v = Number(value) || 0
  return (v < 0 ? '-¥' : '¥') + money(Math.abs(v), dec)
}

function signedPct(value) {
  if (value == null) return '—'
  const n = Number(value)
  return `${n > 0 ? '+' : ''}${n.toFixed(1)}%`
}

function clamp(value, min, max) {
  return Math.min(Math.max(Number(value) || 0, min), max)
}

function scopeAmount(scope) {
  return Number(scopeList.value.find(item => item.scope === scope)?.amount || 0)
}

function scopeColor(scope) {
  return COLORS[scope] || '#98a2b3'
}

function scopeShort(scope) {
  return ({ ALL: '全部', DAILY: '日常', FIXED: '固定', SPECIAL: '专项' })[scope] || scope
}

function rankWidth(c) {
  return `${(Number(c.amount) / maxRankAmount.value) * 100}%`
}

function incomeWidth(c) {
  return `${(Number(c.amount) / maxIncomeAmount.value) * 100}%`
}

function merchantWidth(m) {
  return `${(Number(m.amount) / maxMerchantAmount.value) * 100}%`
}

function rankDelta(c, upIsGood = false) {
  if (!dataStates.value.compareAvailable || c.prevAmount == null) {
    return { text: '—', tone: 'flat' }
  }
  if (Number(c.prevAmount) === 0) {
    return { text: '新增', tone: upIsGood ? 'good' : 'flat' }
  }
  if (c.deltaPct == null) return { text: '—', tone: 'flat' }
  const p = Number(c.deltaPct)
  if (Math.abs(p) < 0.5) return { text: '持平', tone: 'flat' }
  const up = p > 0
  return {
    text: `${up ? '↑' : '↓'}${Math.abs(p).toFixed(0)}%`,
    tone: up === upIsGood ? 'good' : 'bad'
  }
}

function axisFmt(value, useWan = false) {
  if (!value) return '0'
  const v = Number(value)
  if (useWan) return `${(v / 10000).toFixed(v % 10000 === 0 ? 0 : 1)}万`
  return nf0.format(v)
}

function shortMonth(month) {
  const [y, m] = String(month).split('-')
  return Number(m) === 12 ? `${y.slice(2)}/12` : `${Number(m)}月`
}

function reload() {
  const seq = ++requestSeq
  loading.value = true
  router.replace({
    query: {
      ...route.query,
      view: 'annual',
      year: year.value,
      level: level.value
    }
  })
  reportApi.annualOverview({ year: year.value, level: level.value }).then(res => {
    if (seq !== requestSeq) return
    data.value = res.data
    nextTick(renderCharts)
  }).catch(e => {
    if (seq === requestSeq) message.error(e.message || '加载年度复盘失败')
  }).finally(() => {
    if (seq === requestSeq) loading.value = false
  })
}

function toggleMore() {
  showMore.value = !showMore.value
  if (showMore.value) nextTick(renderCharts)
}

function openCategory(category) {
  drawer.value = { show: true, loading: true, category, items: [] }
  reportApi.categoryDetail({
    startDate: `${year.value}-01-01`,
    endDate: period.value.endDate,
    categoryId: category.categoryId,
    scope: 'ALL'
  }).then(res => {
    drawer.value.items = res.data?.categoryDetails || []
  }).catch(() => {
    message.error('加载分类明细失败')
  }).finally(() => {
    drawer.value.loading = false
  })
}

function renderCharts() {
  renderMonthly()
  renderWorth()
}

function renderMonthly() {
  if (!monthlyEl.value || !data.value) {
    if (!monthlyEl.value && monthlyChart) {
      monthlyChart.dispose()
      monthlyChart = null
    }
    return
  }
  const items = monthly.value
  const labels = items.map(m => `${Number(m.month.slice(5))}月`)
  const income = items.map(m => m.future ? null : Number(m.income || 0))
  const expense = items.map(m => m.future ? null : Number(m.expense || 0))
  const net = items.map(m => m.future ? null : Number(m.net || 0))
  const maxValue = Math.max(
    ...income.map(v => Math.abs(v || 0)),
    ...expense.map(v => Math.abs(v || 0)),
    0
  )
  const useWan = maxValue >= 10000
  const base = {
    grid: { left: 6, right: 18, top: 38, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const idx = params[0]?.dataIndex ?? 0
        const m = items[idx]
        if (!m) return ''
        if (m.future) return `<b>${m.label}</b><br>未开始`
        return `<b>${m.label}</b><br>`
          + `收入 ¥${money(m.income, 2)}<br>`
          + `支出 ${signedMoney(m.expense, 2)}<br>`
          + `结余 ${signedMoney(m.net, 2)}<br>`
          + `储蓄率 ${Number(m.savingsRate || 0).toFixed(1)}%`
      }
    },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 9,
      itemHeight: 9,
      textStyle: { color: '#64748b', fontSize: 11 }
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: {
        color: (value, index) => items[index]?.future ? '#cbd5e1' : '#94a3b8',
        fontSize: props.mobile ? 10 : 11
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#94a3b8',
        fontSize: 10,
        formatter: value => axisFmt(value, useWan)
      },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    series: [
      {
        name: '收入',
        type: 'bar',
        data: income,
        barMaxWidth: props.mobile ? 11 : 16,
        itemStyle: { color: '#0ea5a4', borderRadius: [3, 3, 0, 0] }
      },
      {
        name: '支出',
        type: 'bar',
        data: expense,
        barMaxWidth: props.mobile ? 11 : 16,
        itemStyle: { color: '#2a78d6', borderRadius: [3, 3, 0, 0] }
      },
      {
        name: '结余',
        type: 'line',
        data: net,
        symbol: 'circle',
        symbolSize: 5,
        connectNulls: false,
        lineStyle: { width: 2, color: '#0f172a' },
        itemStyle: { color: '#0f172a', borderColor: '#fff', borderWidth: 1.5 },
        z: 5
      }
    ]
  }
  if (!monthlyChart) monthlyChart = echarts.init(monthlyEl.value)
  monthlyChart.setOption(base, true)
}

function renderWorth() {
  if (!worthEl.value || !data.value) {
    if (!worthEl.value && worthChart) {
      worthChart.dispose()
      worthChart = null
    }
    return
  }
  const items = netWorth.value.trend || []
  if (items.length < 2) {
    if (worthChart) {
      worthChart.dispose()
      worthChart = null
    }
    return
  }
  if (!worthChart) worthChart = echarts.init(worthEl.value)
  worthChart.setOption({
    grid: { left: 6, right: 12, top: 16, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      formatter: params => `${params[0]?.name}<br>净资产 ${signedMoney(params[0]?.value, 0)}`
    },
    xAxis: {
      type: 'category',
      data: items.map(item => shortMonth(item.month)),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: {
        color: '#94a3b8',
        fontSize: 10,
        interval: props.mobile ? 1 : 0
      }
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLabel: { color: '#94a3b8', fontSize: 10, formatter: value => axisFmt(value, true) },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    series: [{
      type: 'line',
      data: items.map(item => Number(item.netWorth || 0)),
      symbol: 'circle',
      symbolSize: 5,
      smooth: false,
      lineStyle: { width: 2, color: '#0ea5a4' },
      itemStyle: { color: '#0ea5a4', borderColor: '#fff', borderWidth: 2 },
      areaStyle: { color: 'rgba(14, 165, 164, 0.10)' }
    }]
  }, true)
}

function resizeCharts() {
  monthlyChart?.resize()
  worthChart?.resize()
}

useReportRefresh(reload, { isBusy: () => loading.value })

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
  reload()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  monthlyChart?.dispose()
  worthChart?.dispose()
})
</script>

<style scoped>
.an-page { color: var(--bp-ink); }
.an-head {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: 16px; flex-wrap: wrap; margin-bottom: 16px;
}
.an-head h2 { margin: 0; font-size: 24px; letter-spacing: -0.02em; }
.an-title p { margin: 3px 0 0; font-size: 12px; color: var(--bp-muted); }
.an-filters { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.an-alert { margin-bottom: 12px; }

.an-body { transition: opacity 0.15s; }
.an-body.dim { opacity: 0.55; pointer-events: none; }
.an-card {
  background: var(--bp-surface);
  border: 1px solid var(--bp-line);
  border-radius: 14px;
  padding: 18px;
  box-shadow: var(--bp-shadow);
  min-width: 0;
}
.an-card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.an-card-head h3 { margin: 0; font-size: 15px; letter-spacing: -0.01em; }
.an-cap { margin: 2px 0 0; font-size: 12px; color: var(--bp-muted); line-height: 1.55; }
.an-muted { color: var(--bp-muted); font-size: 12px; }
.an-link {
  border: 0; background: transparent; color: var(--bp-primary);
  font-size: 12px; cursor: pointer; padding: 4px 0; white-space: nowrap;
}
.an-link:hover { text-decoration: underline; }

.an-grid { display: grid; gap: 16px; margin-bottom: 16px; }
.an-grid-8-4 { grid-template-columns: minmax(0, 2fr) minmax(0, 1fr); }
.an-grid-7-5 { grid-template-columns: minmax(0, 7fr) minmax(0, 5fr); }
.an-hero-grid {
  display: grid; grid-template-columns: minmax(0, 1.62fr) minmax(0, 1fr);
  gap: 16px; margin-bottom: 16px;
}

.an-eyebrow { font-size: 11px; font-weight: 700; letter-spacing: 0.12em; color: var(--bp-muted); }
.an-hero-main { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; margin-top: 8px; }
.an-figure {
  font-size: 44px; line-height: 1.08; font-weight: 750; letter-spacing: -0.03em;
}
.an-figure.neg { color: #b42318; }
.an-delta-line { display: flex; align-items: center; gap: 8px; margin-top: 8px; min-height: 20px; flex-wrap: wrap; }
.an-hero-side { text-align: right; font-size: 12px; color: var(--bp-muted); padding-top: 2px; }
.an-hero-side strong { display: block; font-size: 17px; color: var(--bp-ink); margin: 2px 0 10px; }
.an-hero-side strong:last-child { margin-bottom: 0; }
.an-chip {
  display: inline-flex; align-items: center; border-radius: 999px;
  padding: 2px 8px; font-size: 11px; font-weight: 650; white-space: nowrap;
  background: #f1f5f9; color: #64748b;
}
.an-chip.good { background: #ecfdf3; color: #067647; }
.an-chip.bad { background: #fef3f2; color: #b42318; }
.an-chip.warn { background: #fffaeb; color: #b54708; }
.an-chip.neutral { background: #eff6ff; color: #2563eb; }
.an-chip.flat { background: #f1f5f9; color: #64748b; }

.an-segbar { display: flex; gap: 2px; height: 26px; margin-top: 18px; border-radius: 6px; overflow: hidden; }
.an-segbar button {
  border: 0; padding: 0 0 0 9px; display: flex; align-items: center;
  color: #fff; font-size: 11px; font-weight: 700; min-width: 0; overflow: hidden; white-space: nowrap;
  cursor: default;
}
.an-seglegend { display: flex; align-items: center; gap: 16px; margin-top: 10px; flex-wrap: wrap; }
.an-leg { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #64748b; }
.an-leg i { width: 8px; height: 8px; border-radius: 3px; display: block; }
.an-leg b { color: var(--bp-ink); font-weight: 650; }
.an-seg-total { margin-left: auto; font-size: 12px; color: var(--bp-muted); white-space: nowrap; }
.an-seg-total strong { color: var(--bp-ink); }

.an-stats { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.an-stat {
  background: var(--bp-surface); border: 1px solid var(--bp-line); border-radius: 14px;
  padding: 15px 16px 13px; box-shadow: var(--bp-shadow);
  display: flex; flex-direction: column; justify-content: space-between; min-height: 104px;
}
.an-stat-label { font-size: 12px; color: var(--bp-muted); }
.an-stat-value { margin-top: 4px; font-size: 22px; font-weight: 730; letter-spacing: -0.02em; }
.an-stat-foot { display: flex; align-items: center; gap: 6px; margin-top: 8px; flex-wrap: wrap; }

.an-chart { width: 100%; }
.an-table { width: 100%; border-collapse: collapse; margin-top: 12px; font-size: 12px; }
.an-table th, .an-table td { padding: 7px 8px; border-bottom: 1px solid var(--bp-line); text-align: left; white-space: nowrap; }
.an-table th { color: var(--bp-muted); font-weight: 600; font-size: 11px; background: #fafbfc; }
.an-table .n { text-align: right; font-variant-numeric: tabular-nums; }
.an-table tr.future td { color: #cbd5e1; }
.an-month-foot { display: flex; gap: 16px; flex-wrap: wrap; margin-top: 8px; font-size: 12px; color: var(--bp-muted); }
.an-month-foot b { color: var(--bp-ink); }

.an-health { display: flex; flex-direction: column; gap: 15px; margin-top: 16px; }
.an-health-row { min-width: 0; }
.an-health-top { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; font-size: 12px; color: var(--bp-muted); }
.an-health-top b { font-size: 18px; color: var(--bp-ink); font-variant-numeric: tabular-nums; }
.an-health-top b.good { color: #067647; }
.an-health-top b.warn { color: #b54708; }
.an-health-top b.bad { color: #b42318; }
.an-health-track { height: 7px; border-radius: 999px; background: #eef2f6; overflow: hidden; margin-top: 6px; }
.an-health-track span { display: block; height: 100%; border-radius: 999px; background: #2a78d6; }
.an-health-track span.good { background: #1baf7a; }
.an-health-track span.warn { background: #f79009; }
.an-health-track span.bad { background: #d03b3b; }
.an-health-track span.neutral { background: #2a78d6; }
.an-health-note { margin-top: 4px; font-size: 11px; color: var(--bp-muted); line-height: 1.5; }

.an-worth-top { display: flex; gap: 24px; flex-wrap: wrap; margin-top: 14px; }
.an-worth-top span { display: block; font-size: 11px; color: var(--bp-muted); }
.an-worth-top strong { display: block; margin-top: 2px; font-size: 18px; font-variant-numeric: tabular-nums; }
.an-worth-top strong small { font-size: 11px; font-weight: 500; color: var(--bp-muted); }
.an-worth-top strong.good { color: #067647; }
.an-worth-top strong.bad { color: #b42318; }
.an-worth-stats {
  display: grid; grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px; margin-top: 10px; padding-top: 12px; border-top: 1px solid var(--bp-line);
}
.an-worth-stats span { display: block; font-size: 11px; color: var(--bp-muted); }
.an-worth-stats b { display: block; margin-top: 3px; font-size: 13px; font-variant-numeric: tabular-nums; }
.an-worth-stats b.bad { color: #b42318; }
.an-worth-groups {
  display: flex; flex-wrap: wrap; gap: 6px 14px; margin-top: 10px;
  padding-top: 10px; border-top: 1px dashed var(--bp-line);
  font-size: 11px; color: var(--bp-muted);
}
.an-worth-groups b { color: var(--bp-ink); font-variant-numeric: tabular-nums; }
.an-worth-groups b.bad { color: #b42318; }
.an-note {
  margin: 10px 0 0; padding: 9px 11px; border-radius: 8px;
  background: #f8fafc; font-size: 11px; line-height: 1.65; color: var(--bp-muted);
}

.an-side-stack { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
.an-budget-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 10px; margin-top: 14px; }
.an-budget-value { font-size: 22px; font-weight: 750; letter-spacing: -0.02em; }
.an-budget-value small { font-size: 11px; font-weight: 500; color: var(--bp-muted); letter-spacing: 0; }
.an-pct {
  font-size: 12px; font-weight: 700; border-radius: 999px; padding: 3px 9px;
  background: #ecfdf3; color: #067647; white-space: nowrap;
}
.an-pct.bad { background: #fef3f2; color: #b42318; }
.an-pct.warn { background: #fffaeb; color: #b54708; }
.an-pct.ok { background: #ecfdf3; color: #067647; }
.an-meter { position: relative; height: 10px; border-radius: 999px; background: #eef2f6; margin-top: 10px; }
.an-meter > span { display: block; height: 100%; border-radius: 999px; background: #2a78d6; min-width: 2px; }
.an-meter > span.bad { background: #d03b3b; }
.an-meter > span.warn { background: #f79009; }
.an-meter > span.ok { background: #2a78d6; }
.an-time-mark { position: absolute; top: -4px; bottom: -4px; width: 2px; background: #0f172a; border-radius: 2px; }
.an-meter-cap { display: flex; justify-content: space-between; margin-top: 6px; }
.an-budget-list { margin-top: 14px; display: flex; flex-direction: column; gap: 10px; }
.an-brow { display: grid; grid-template-columns: 64px minmax(0, 1fr) 44px; gap: 10px; align-items: center; font-size: 12px; }
.an-brow-name { color: #475467; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.an-brow-track { height: 7px; border-radius: 999px; background: #eef2f6; overflow: hidden; }
.an-brow-track span { display: block; height: 100%; border-radius: 999px; background: #2a78d6; }
.an-brow-track span.done { background: #1baf7a; }
.an-brow-track span.warn { background: #f79009; }
.an-brow-val { text-align: right; font-variant-numeric: tabular-nums; color: var(--bp-muted); }
.an-more { margin-top: 10px; font-size: 11px; }
.an-mini-stats { display: flex; gap: 18px; flex-wrap: wrap; margin: 14px 0 8px; }
.an-mini-stats span { display: block; font-size: 11px; color: var(--bp-muted); }
.an-mini-stats b { display: block; margin-top: 2px; font-size: 16px; font-variant-numeric: tabular-nums; }
.an-mini-stats b.bad { color: #b42318; }
.an-mini-stats b.good { color: #067647; }
.an-over-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.an-over-chip {
  border-radius: 999px; padding: 2px 8px; font-size: 11px;
  background: #fef3f2; color: #b42318;
}

.an-rank-head, .an-rank-row {
  display: grid;
  grid-template-columns: 22px 10px minmax(96px, 1.05fr) minmax(80px, 1.6fr) 92px 58px;
  gap: 10px; align-items: center;
}
.an-rank-head {
  margin-top: 14px; padding: 0 4px 6px; border-bottom: 1px solid var(--bp-line);
  font-size: 11px; color: var(--bp-muted);
}
.an-rank-row {
  width: 100%; border: 0; background: transparent; text-align: left;
  padding: 9px 4px; border-bottom: 1px solid #f5f7fa; cursor: pointer;
}
.an-rank-row:hover { background: #f8fbff; }
.an-rank-no { font-size: 11px; color: var(--bp-muted); }
.an-dot { width: 8px; height: 8px; border-radius: 3px; display: block; }
.an-rank-name { min-width: 0; }
.an-rank-name .nm { display: block; font-size: 13px; font-weight: 600; color: var(--bp-ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.an-rank-name .meta { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 2px; font-size: 11px; color: var(--bp-muted); }
.an-rank-bar { height: 7px; border-radius: 999px; background: #eef2f6; overflow: hidden; }
.an-rank-bar span { display: block; height: 100%; border-radius: 999px; background: #62a0e8; }
.an-rank-amt { text-align: right; font-size: 12px; font-variant-numeric: tabular-nums; font-weight: 600; }
.an-rank-delta { text-align: right; font-size: 11px; font-variant-numeric: tabular-nums; white-space: nowrap; }
.an-rank-delta.good { color: #067647; }
.an-rank-delta.bad { color: #b42318; }
.an-rank-delta.flat { color: var(--bp-muted); }

.an-income-row {
  display: grid; grid-template-columns: minmax(84px, 1.2fr) minmax(60px, 1.2fr) 92px 54px;
  gap: 10px; align-items: center; padding: 10px 0; border-bottom: 1px solid #f5f7fa;
}
.an-income-row:first-of-type { margin-top: 6px; }
.an-income-main { min-width: 0; }
.an-income-main .nm { display: block; font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.an-income-main .meta { display: block; margin-top: 2px; font-size: 11px; color: var(--bp-muted); }
.an-income-bar { height: 7px; border-radius: 999px; background: #eef2f6; overflow: hidden; }
.an-income-bar span { display: block; height: 100%; border-radius: 999px; }
.an-income-amt { text-align: right; font-size: 12px; font-weight: 600; font-variant-numeric: tabular-nums; }
.an-income-amt small { display: block; font-size: 10px; font-weight: 500; color: var(--bp-muted); }

.an-merchant-row {
  display: grid; grid-template-columns: minmax(130px, 1.2fr) minmax(80px, 1.8fr) 92px;
  gap: 12px; align-items: center; padding: 10px 0; border-bottom: 1px solid #f5f7fa;
}
.an-merchant-main { min-width: 0; }
.an-merchant-name { font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.an-merchant-meta { display: flex; gap: 7px; flex-wrap: wrap; margin-top: 3px; font-size: 11px; color: var(--bp-muted); }
.an-badge { border-radius: 4px; padding: 1px 5px; background: #f1f5f9; color: #64748b; }
.an-merchant-bar { height: 7px; border-radius: 999px; background: #eef2f6; overflow: hidden; }
.an-merchant-bar span { display: block; height: 100%; border-radius: 999px; background: #2a78d6; }
.an-merchant-amt { text-align: right; font-size: 12px; font-weight: 600; font-variant-numeric: tabular-nums; }

.an-moments { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 14px; }
.an-moment { padding: 11px 12px; border-radius: 10px; background: #f8fafc; border: 1px solid #eef2f7; min-width: 0; }
.an-moment-label { font-size: 11px; color: var(--bp-muted); }
.an-moment-value { margin-top: 3px; font-size: 17px; font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.an-moment-note { margin-top: 2px; font-size: 11px; color: var(--bp-muted); line-height: 1.5; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.an-insights { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-bottom: 16px; }
.an-insight {
  background: var(--bp-surface); border: 1px solid var(--bp-line); border-radius: 14px;
  padding: 15px; box-shadow: var(--bp-shadow); display: flex; gap: 11px;
}
.an-insight-icon {
  width: 32px; height: 32px; border-radius: 10px; display: grid; place-items: center;
  flex: 0 0 auto; background: #eff6ff; color: #2563eb;
}
.an-insight-icon.bad { background: #fef3f2; color: #b42318; }
.an-insight-icon.warn { background: #fffaeb; color: #b54708; }
.an-insight-icon.good { background: #ecfdf3; color: #067647; }
.an-insight h4 { margin: 0; font-size: 13px; }
.an-insight p { margin: 4px 0 0; font-size: 12px; line-height: 1.65; color: #475467; }
.an-foot { font-size: 11px; line-height: 1.7; color: var(--bp-muted); margin: 0 0 8px; }

.an-drawer-total { font-size: 28px; font-weight: 750; letter-spacing: -0.02em; }
.an-drawer-sec { margin-top: 20px; }
.an-drawer-sec h4 { margin: 0 0 8px; font-size: 12px; color: var(--bp-muted); font-weight: 600; }
.an-drawer-tx { display: flex; justify-content: space-between; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f2f4f7; }
.an-drawer-tx-note { font-size: 12px; color: #334155; }
.an-drawer-tx-amt { font-size: 12px; font-weight: 650; font-variant-numeric: tabular-nums; white-space: nowrap; color: #b42318; }
.an-drawer-tx-amt.refund { color: #067647; }
.an-drawer-note { margin-top: 18px; padding: 10px 12px; background: #f8fafc; border-radius: 8px; font-size: 11px; line-height: 1.65; color: var(--bp-muted); }

.mobile .an-grid-8-4,
.mobile .an-grid-7-5,
.mobile .an-hero-grid,
.mobile .an-insights { grid-template-columns: 1fr; }
.mobile .an-worth-stats { grid-template-columns: 1fr 1fr; }
.mobile .an-head { align-items: flex-start; }
.mobile .an-filters { width: 100%; }
.mobile .an-filter-year { flex: 1 1 auto; width: auto !important; }
.mobile .an-filter-level { flex: 0 0 auto; display: flex; }
.mobile .an-filter-level :deep(.n-radio-button) { text-align: center; }
.mobile .an-title h2 { display: none; }
.mobile .an-title p { display: none; }
.mobile .an-figure { font-size: 34px; }
.mobile .an-hero-main { flex-direction: column; gap: 8px; }
.mobile .an-hero-side { text-align: left; display: flex; gap: 24px; }
.mobile .an-hero-side strong { margin-bottom: 0; }
.mobile .an-stats { gap: 10px; }
.mobile .an-stat { min-height: 94px; padding: 12px 13px; }
.mobile .an-stat-value { font-size: 19px; }
.mobile .an-rank-head, .mobile .an-rank-row {
  grid-template-columns: 16px 6px minmax(64px, 1.2fr) minmax(36px, 0.9fr) 78px 48px;
}
.mobile .an-income-row { grid-template-columns: minmax(70px, 1.2fr) minmax(36px, 0.9fr) 78px 44px; }
.mobile .an-merchant-row { grid-template-columns: minmax(100px, 1.4fr) minmax(40px, 1fr) 70px; }
.mobile .an-moments { grid-template-columns: 1fr; }
.mobile .an-card { padding: 15px; }
.an-page.mobile { padding-bottom: calc(84px + env(safe-area-inset-bottom, 0px)); }

@media (max-width: 1100px) {
  .an-grid-8-4, .an-grid-7-5, .an-hero-grid { grid-template-columns: 1fr; }
  .an-insights { grid-template-columns: 1fr; }
}
@media (max-width: 720px) {
  .an-head h2 { font-size: 22px; }
  .an-figure { font-size: 34px; }
  .an-hero-main { flex-direction: column; gap: 8px; }
  .an-hero-side { text-align: left; display: flex; gap: 24px; }
  .an-hero-side strong { margin-bottom: 0; }
  .an-stats { grid-template-columns: 1fr 1fr; }
  .an-stat-value { font-size: 19px; }
  .an-worth-stats { grid-template-columns: 1fr 1fr; }
  .an-rank-head, .an-rank-row {
    grid-template-columns: 16px 6px minmax(64px, 1.2fr) minmax(36px, 0.9fr) 78px 48px;
  }
  .an-card { padding: 15px; }
}
</style>