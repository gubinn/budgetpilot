<template>
  <div class="report-page" :class="{ mobile }">
    <div class="rp-head">
      <div class="rp-title">
        <h2>{{ mobile ? '报表' : '月度复盘' }}</h2>
        <p v-if="data">
          {{ data.period.label }}
          <span v-if="data.period.compareLabel"> · 对比 {{ data.period.compareLabel }}</span>
        </p>
        <p v-else>加载中…</p>
      </div>
      <div class="rp-filters">
        <n-select v-model:value="month" :options="monthOptions" size="small" style="width: 132px" @update:value="reload" />
        <n-select v-model:value="compare" :options="compareOptions" size="small" style="width: 116px" @update:value="reload" />
        <n-radio-group v-model:value="scope" size="small" @update:value="reload">
          <n-radio-button v-for="s in scopeOptions" :key="s.value" :value="s.value">{{ s.label }}</n-radio-button>
        </n-radio-group>
        <n-radio-group v-model:value="level" size="small" @update:value="reload">
          <n-radio-button :value="1">一级</n-radio-button>
          <n-radio-button :value="2">二级</n-radio-button>
        </n-radio-group>
      </div>
    </div>

    <n-alert v-if="data?.dataStates?.partialMonth" type="info" :bordered="false" class="rp-alert">
      本月还没过完，数据截至今天；环比按上一个自然月。
    </n-alert>
    <n-alert v-if="data?.dataStates?.noData" type="warning" :bordered="false" class="rp-alert">
      本期没有支出记录。换个区间，或确认流水是否已入账。
    </n-alert>

    <n-spin :show="loading">
      <div v-if="data" class="rp-body" :class="{ dim: loading }">
        <!-- 主卡 + KPI -->
        <section class="rp-hero-grid">
          <article class="rp-card rp-hero">
            <div class="rp-hero-top">
              <div class="rp-hero-main">
                <div class="rp-eyebrow">本期{{ scopeLabel }}</div>
                <div class="rp-figure"><span class="cur">¥</span>{{ money(kpis.expense, 2) }}</div>
                <div class="rp-delta-line">
                  <span class="rp-delta-chip" :class="heroDelta.tone">{{ heroDelta.text }}</span>
                  <span class="rp-muted">{{ heroDelta.note }}</span>
                </div>
              </div>
              <div class="rp-hero-total">
                <span>{{ scope === 'ALL' ? '其中日常' : '全部支出' }}</span>
                <strong>{{ money(scope === 'ALL' ? scopeAmount('DAILY') : kpis.expenseAll, 0) }}</strong>
              </div>
            </div>
            <div class="rp-segbar" role="group" aria-label="支出结构">
              <button
                v-for="s in scopeList.filter(x => Number(x.amount) > 0.004)"
                :key="s.scope"
                :style="{ flex: Math.max(s.amount, 0.01), background: scopeColor(s.scope) }"
                :title="`${scopeShort(s.scope)} ${money(s.amount, 0)} · ${s.share}%`"
                @click="setScope(s.scope)"
              >
                <span v-if="s.share >= 12">{{ s.share }}%</span>
              </button>
            </div>
            <div class="rp-seglegend">
              <button
                v-for="s in scopeList"
                :key="s.scope"
                class="rp-leg"
                :class="{ sel: scope === s.scope }"
                @click="setScope(s.scope)"
              >
                <i :style="{ background: scopeColor(s.scope) }"></i>
                <span>{{ scopeShort(s.scope) }}</span>
                <b>{{ money(s.amount, 0) }}</b>
                <span class="rp-muted">{{ s.share }}%</span>
              </button>
              <div class="rp-seg-total">合计 <strong>{{ money(kpis.expenseAll, 0) }}</strong></div>
            </div>
          </article>

          <div class="rp-stats">
            <div v-for="s in stats" :key="s.label" class="rp-stat">
              <div>
                <div class="rp-stat-label">{{ s.label }}</div>
                <div class="rp-stat-value">{{ s.value }}</div>
              </div>
              <div class="rp-stat-foot">
                <span class="rp-delta-chip" :class="s.delta.tone">{{ s.delta.text }}</span>
                <span class="rp-muted">{{ s.delta.note }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 趋势 + 预算 -->
        <section class="rp-grid rp-grid-8-4">
          <article class="rp-card">
            <div class="rp-card-head">
              <div>
                <h3>支出趋势</h3>
                <p class="rp-cap">近 12 个月 · {{ scopeLabel }}{{ scope === 'DAILY' && data.budget.exists ? ' · 虚线为月度预算' : '' }}</p>
              </div>
              <button class="rp-link" @click="showTrendTable = !showTrendTable">
                {{ showTrendTable ? '收起数据表' : '数据表' }}
              </button>
            </div>
            <div ref="trendEl" class="rp-chart" :style="{ height: mobile ? '230px' : '290px' }"></div>
            <table v-if="showTrendTable" class="rp-table">
              <thead><tr><th>月份</th><th class="n">日常</th><th class="n">固定</th><th class="n">专项</th><th class="n">合计</th></tr></thead>
              <tbody>
                <tr v-for="t in data.trend" :key="t.month" :class="{ sel: t.month === month }">
                  <td>{{ t.month }}</td>
                  <td class="n">¥{{ money(t.daily) }}</td>
                  <td class="n">¥{{ money(t.fixed) }}</td>
                  <td class="n">¥{{ money(t.special) }}</td>
                  <td class="n">¥{{ money(t.expense) }}</td>
                </tr>
              </tbody>
            </table>
          </article>

          <article class="rp-card">
            <div class="rp-card-head">
              <div>
                <h3>日常预算执行</h3>
                <p class="rp-cap">{{ data.budget.exists ? `${data.budget.month} 月度预算` : '所选月份未设置预算' }}</p>
              </div>
            </div>
            <template v-if="data.budget.exists">
              <div class="rp-budget-hero">
                <div>
                  <div class="rp-budget-value">¥{{ money(data.budget.spent, 0) }}<small> / ¥{{ money(data.budget.total, 0) }}</small></div>
                  <div class="rp-cap">
                    {{ Number(data.budget.remaining) < 0 ? '超支' : '剩余' }}
                    <b :class="Number(data.budget.remaining) < 0 ? 'rp-bad' : 'rp-good'">¥{{ money(Math.abs(data.budget.remaining), 0) }}</b>
                  </div>
                </div>
                <span class="rp-pct" :class="budgetTone">{{ data.budget.pct }}%</span>
              </div>
              <div class="rp-meter"><span :class="budgetTone" :style="{ width: Math.min(data.budget.pct, 100) + '%' }"></span></div>
              <div class="rp-budget-list">
                <div v-for="b in budgetItems.slice(0, 5)" :key="b.categoryId" class="rp-brow">
                  <span class="rp-brow-name">{{ b.name }}</span>
                  <span class="rp-brow-track"><span :class="b.pct > 100 ? 'bad' : b.pct > 90 ? 'warn' : ''" :style="{ width: Math.min(b.pct, 100) + '%' }"></span></span>
                  <span class="rp-brow-val"><b>{{ Number(b.pct).toFixed(0) }}%</b></span>
                </div>
              </div>
              <div v-if="budgetItems.length > 5" class="rp-cap rp-budget-more">
                其余 {{ budgetItems.length - 5 }} 项：{{ budgetItems.slice(5).map(b => `${b.name} ${Number(b.pct).toFixed(0)}%`).join(' · ') }}
              </div>
            </template>
            <n-empty v-else description="所选月份未设置月度预算">
              <template #extra>
                <n-button size="small" type="primary" @click="router.push('/budget')">去设置预算</n-button>
              </template>
            </n-empty>
          </article>
        </section>

        <!-- 分类排行 + 日历 -->
        <section class="rp-grid rp-grid-7-5">
          <article class="rp-card">
            <div class="rp-card-head">
              <div>
                <h3>分类排行</h3>
                <p class="rp-cap">按金额降序 · 点任意一行查看明细</p>
              </div>
              <button class="rp-link" @click="showRankTable = !showRankTable">
                {{ showRankTable ? '收起数据表' : '数据表' }}
              </button>
            </div>
            <div class="rp-rank-head">
              <span>#</span><span></span><span>分类</span><span>金额分布</span>
              <span class="n">金额</span><span class="n">占比</span><span class="n">环比</span>
            </div>
            <button
              v-for="(c, i) in data.categories"
              :key="c.categoryId"
              class="rp-rank-row"
              @click="openCategory(c)"
            >
              <span class="rp-rank-no">{{ i + 1 }}</span>
              <i class="rp-dot" :style="{ background: c.color || '#98a2b3' }"></i>
              <span class="rp-rank-name">
                <span class="nm">{{ c.name }}</span>
                <span class="meta">
                  {{ c.count }} 笔<span v-if="scope === 'ALL'"> · {{ scopeShort(c.scope) }}</span><span v-if="c.parentName"> · {{ c.parentName }}</span>
                  <span v-if="c.budgetPct != null" class="rp-budget-chip" :class="c.budgetPct > 100 ? 'bad' : c.budgetPct > 90 ? 'warn' : ''">
                    预算 {{ Number(c.budgetPct).toFixed(0) }}%
                  </span>
                </span>
              </span>
              <span class="rp-rank-bar"><span :style="{ width: rankWidth(c) }"></span></span>
              <span class="rp-rank-amt">¥{{ money(c.amount) }}</span>
              <span class="rp-rank-share">{{ c.share }}%</span>
              <span class="rp-rank-delta" :class="rankDelta(c).tone">{{ rankDelta(c).text }}</span>
            </button>
            <p v-if="!data.categories.length" class="rp-cap" style="padding: 18px 4px">当前范围内没有支出</p>
            <table v-if="showRankTable" class="rp-table">
              <thead><tr><th>#</th><th>分类</th><th class="n">金额</th><th class="n">占比</th><th class="n">笔数</th><th class="n">环比</th></tr></thead>
              <tbody>
                <tr v-for="(c, i) in data.categories" :key="c.categoryId">
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

          <article class="rp-card">
            <div class="rp-card-head">
              <div>
                <h3>消费日历</h3>
                <p class="rp-cap">{{ data.period.label }} · {{ scopeLabel }}</p>
              </div>
              <div class="rp-heat-legend"><span>少</span><i v-for="n in 5" :key="n" :class="`h${n}`"></i><span>多</span></div>
            </div>
            <div class="rp-cal">
              <div v-for="w in weekdays" :key="w" class="rp-cal-wd">{{ w }}</div>
              <div
                v-for="(c, i) in calendar.cells"
                :key="i"
                class="rp-cal-cell"
                :class="c.empty ? 'empty' : `l${c.level}`"
                :title="c.empty ? '' : `${c.date} · ¥${money(c.value)}`"
              >
                <template v-if="!c.empty">
                  <span class="d">{{ c.day }}</span>
                  <span class="a">{{ c.display }}</span>
                </template>
              </div>
            </div>
            <div class="rp-cal-foot">
              <span>有消费 <b>{{ calendar.active }}</b> 天</span>
              <span>消费日均 <b>¥{{ money(calendar.avg, 0) }}</b></span>
              <span>最高 <b>¥{{ money(calendar.top, 0) }}</b><template v-if="calendar.top > 0">（{{ calendar.topDay }} 日）</template></span>
              <span>最高 3 天占 <b>{{ calendar.top3Share.toFixed(0) }}%</b></span>
            </div>
          </article>
        </section>

        <!-- 商户 + 年度 -->
        <section class="rp-grid rp-grid-7-5">
          <article class="rp-card">
            <div class="rp-card-head">
              <div>
                <h3>商户排行</h3>
                <p class="rp-cap">{{ scopeLabel }} · 前 {{ Math.min(data.merchants.length, 8) }} 名</p>
              </div>
            </div>
            <div v-for="m in data.merchants.slice(0, 8)" :key="m.merchantId" class="rp-merchant-row">
              <div class="rp-merchant-main">
                <div class="rp-merchant-name">{{ m.name }}</div>
                <div class="rp-merchant-meta">
                  <span class="rp-badge">{{ m.categoryName || '未分类' }}</span>
                  <span>{{ m.count }} 笔</span>
                  <span v-if="m.count >= 5" class="rp-badge hot">常客</span>
                </div>
              </div>
              <div class="rp-merchant-bar"><span :style="{ width: merchantWidth(m) }"></span></div>
              <div class="rp-merchant-amt">¥{{ money(m.amount, 0) }}</div>
            </div>
            <n-empty v-if="!data.merchants.length" description="当前范围内没有商户数据" />
          </article>

          <article v-if="!mobile || showMore" class="rp-card">
            <div class="rp-card-head">
              <div>
                <h3>年度预算执行</h3>
                <p class="rp-cap">固定支出 + 年度专项 · 全年口径</p>
              </div>
            </div>
            <template v-if="data.annualBudget.items?.length">
              <div class="rp-annual-total">
                <div>
                  <div class="rp-budget-value">¥{{ money(data.annualBudget.spent, 0) }}<small> / ¥{{ money(data.annualBudget.total, 0) }}</small></div>
                  <div class="rp-cap">
                    {{ Number(data.annualBudget.remaining) < 0 ? '超支' : '剩余' }}
                    ¥{{ money(Math.abs(data.annualBudget.remaining), 0) }} · 时间进度 {{ data.annualBudget.timePct }}%
                  </div>
                </div>
                <span class="rp-pct" :class="annualTone">{{ data.annualBudget.pct }}%</span>
              </div>
              <div class="rp-meter">
                <span :class="annualTone" :style="{ width: Math.min(data.annualBudget.pct, 100) + '%' }"></span>
                <i class="rp-time-mark" :style="{ left: Math.min(data.annualBudget.timePct, 100) + '%' }" :title="`时间进度 ${data.annualBudget.timePct}%`"></i>
              </div>
              <div class="rp-cap" style="display:flex;justify-content:space-between;margin-top:6px">
                <span>花费进度 {{ data.annualBudget.pct }}%</span><span>时间进度 {{ data.annualBudget.timePct }}%</span>
              </div>
              <div class="rp-annual-list">
                <div v-for="a in data.annualBudget.items" :key="a.categoryId" class="rp-arow">
                  <span class="rp-arow-name">
                    <i v-if="a.pct >= 100" class="rp-check">✓</i>{{ a.name }}
                  </span>
                  <span class="rp-arow-track"><span :class="a.pct >= 100 ? 'done' : a.pct >= 90 ? 'warn' : ''" :style="{ width: Math.min(a.pct, 100) + '%' }"></span></span>
                  <span class="rp-arow-val">{{ Number(a.pct).toFixed(0) }}%</span>
                </div>
              </div>
            </template>
            <n-empty v-else description="未设置年度预算">
              <template #extra>
                <n-button size="small" @click="router.push('/budget')">去设置</n-button>
              </template>
            </n-empty>
          </article>
        </section>

        <n-button v-if="mobile && !showMore" block secondary style="margin-bottom: 16px" @click="showMore = true">
          展开年度预算与账户
        </n-button>

        <!-- 账户 -->
        <article v-if="!mobile || showMore" class="rp-card rp-accounts">
          <div class="rp-card-head">
            <div>
              <h3>账户与资产</h3>
              <p class="rp-cap">当前余额 · 信用卡为负值</p>
            </div>
          </div>
          <div class="rp-acct-top">
            <div>
              <div class="rp-cap">净资产</div>
              <div class="rp-networth">{{ signedMoney(data.accounts.netWorth, 0) }}</div>
              <div class="rp-split-legend">
                <span>资产 <b>¥{{ money(data.accounts.assets, 0) }}</b></span>
                <span>负债 <b class="rp-bad">¥{{ money(data.accounts.liabilities, 0) }}</b></span>
              </div>
            </div>
            <div>
              <div class="rp-acct-split">
                <span :style="{ flex: Math.max(data.accounts.assets, 1), background: '#2a78d6' }"></span>
                <span :style="{ flex: Math.max(data.accounts.liabilities, 1), background: '#eb6834' }"></span>
              </div>
              <div class="rp-split-legend">
                <span><i class="rp-dot" style="background:#2a78d6"></i>资产</span>
                <span><i class="rp-dot" style="background:#eb6834"></i>负债</span>
              </div>
            </div>
          </div>
          <div class="rp-acct-groups">
            <div v-for="g in data.accounts.groups" :key="g.type" class="rp-acct-group">
              <div class="rp-cap">{{ g.type }}</div>
              <div class="v" :class="{ neg: Number(g.balance) < 0 }">{{ signedMoney(g.balance, 0) }}</div>
            </div>
          </div>
          <div class="rp-acct-body">
            <div class="rp-acct-list">
              <div v-for="a in data.accounts.accounts" :key="a.id" class="rp-acct-row">
                <span class="nm">{{ a.name }}</span>
                <span class="amt" :class="{ neg: Number(a.balance) < 0 }">{{ signedMoney(a.balance, 0) }}</span>
              </div>
            </div>
            <div class="rp-networth-chart-wrap">
              <div class="rp-cap" style="display:flex;align-items:center;justify-content:space-between;gap:8px">
                <span>近 12 个月净资产 · 月末快照</span>
                <button class="rp-link" @click="showNwTable = !showNwTable">
                  {{ showNwTable ? '收起' : '数据表' }}
                </button>
              </div>
              <div ref="netWorthEl" class="rp-chart" :style="{ height: mobile ? '150px' : '180px' }"></div>
              <table v-if="showNwTable" class="rp-table">
                <thead><tr><th>月份</th><th class="n">净资产</th><th class="n">环比</th></tr></thead>
                <tbody>
                  <tr v-for="(n, i) in nwRows" :key="n.month">
                    <td>{{ n.month }}<span v-if="i === nwRows.length - 1 && n.month === currentMonth" class="rp-muted">（本月实时）</span></td>
                    <td class="n">{{ signedMoney(n.netWorth, 0) }}</td>
                    <td class="n" :class="n.change > 0 ? 'rp-good' : n.change < 0 ? 'rp-bad' : ''">
                      {{ n.change == null ? '—' : (n.change >= 0 ? '+' : '') + signedMoney(n.change, 0) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </article>

        <!-- 洞察 -->
        <section class="rp-insights">
          <div v-for="(x, i) in data.insights" :key="i" class="rp-insight">
            <div class="rp-insight-icon" :class="x.tone">
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

        <p class="rp-foot">
          口径：支出只统计已入账且已复核的流水，退款冲减；固定 / 专项按分类的「预算类型」归集；
          转账、往来款、余额调整不计入收支。
          <template v-if="data.dataStates.multiCurrency"> 本期含外币交易，金额按入账时汇率折算为 CNY。</template>
        </p>
      </div>
      <n-empty v-else-if="!loading" description="暂无报表数据" />
    </n-spin>

    <n-drawer
      v-model:show="drawer.show"
      :placement="mobile ? 'bottom' : 'right'"
      :height="mobile ? '82%' : undefined"
      :width="mobile ? undefined : 440"
    >
      <n-drawer-content :title="drawer.category ? drawer.category.name : '分类明细'" closable>
        <template v-if="drawer.category">
          <div class="rp-drawer-total">¥{{ money(drawer.category.amount) }}</div>
          <div class="rp-cap">
            占当前范围 {{ drawer.category.share }}% · {{ drawer.category.count }} 笔
            <span v-if="drawer.category.parentName"> · {{ drawer.category.parentName }}</span>
          </div>

          <div v-if="drawer.category.children?.length" class="rp-drawer-sec">
            <h4>二级分类</h4>
            <div v-for="s in drawer.category.children" :key="s.categoryId" class="rp-drawer-row">
              <div class="nm"><span>{{ s.name }}</span><span>¥{{ money(s.amount, 0) }}</span></div>
              <div class="rp-drawer-bar"><span :style="{ width: drawerChildWidth(s) }"></span></div>
            </div>
          </div>

          <div class="rp-drawer-sec">
            <h4>交易明细</h4>
            <n-spin :show="drawer.loading">
              <div v-for="t in drawer.items" :key="t.id" class="rp-drawer-tx">
                <div>
                  <div class="rp-drawer-tx-note">{{ t.note || t.accountName || '—' }}</div>
                  <div class="rp-cap">{{ t.transactionDate }}<template v-if="t.accountName"> · {{ t.accountName }}</template></div>
                </div>
                <div class="rp-drawer-tx-amt" :class="{ refund: t.type === 4 }">
                  {{ t.type === 4 ? '+' : '-' }}¥{{ money(t.amount) }}
                </div>
              </div>
              <n-empty v-if="!drawer.loading && !drawer.items.length" description="暂无交易" />
            </n-spin>
          </div>
          <div class="rp-drawer-note">
            下钻已能定位到具体交易。后续可以在这里直接跳转交易列表，按商户、标签、账户继续筛选。
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

const props = defineProps({
  mobile: { type: Boolean, default: false }
})

const route = useRoute()
const router = useRouter()
const message = useMessage()

const COLORS = { DAILY: '#2a78d6', FIXED: '#eb6834', SPECIAL: '#1baf7a' }

const month = ref(route.query.month || dayjs().format('YYYY-MM'))
const scope = ref(route.query.scope || 'DAILY')
const level = ref(Number(route.query.level) || 1)
const compare = ref(route.query.compare || 'PREV')
const data = ref(null)
const loading = ref(false)
const showMore = ref(false)
const showTrendTable = ref(false)
const showRankTable = ref(false)
const showNwTable = ref(false)
const currentMonth = dayjs().format('YYYY-MM')
const drawer = ref({ show: false, loading: false, category: null, items: [] })

const trendEl = ref(null)
const netWorthEl = ref(null)
let trendChart = null
let netWorthChart = null
let requestSeq = 0

const monthOptions = computed(() => Array.from({ length: 12 }, (_, i) => {
  const m = dayjs().subtract(i, 'month')
  return { label: m.format('YYYY年MM月'), value: m.format('YYYY-MM') }
}))
const compareOptions = [
  { label: '环比上月', value: 'PREV' },
  { label: '同比去年', value: 'YOY' },
  { label: '不对比', value: 'NONE' }
]
const scopeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '日常', value: 'DAILY' },
  { label: '固定', value: 'FIXED' },
  { label: '专项', value: 'SPECIAL' }
]
const weekdays = ['一', '二', '三', '四', '五', '六', '日']

const range = computed(() => {
  const m = dayjs(`${month.value}-01`)
  return {
    startDate: m.startOf('month').format('YYYY-MM-DD'),
    endDate: m.endOf('month').format('YYYY-MM-DD')
  }
})
const kpis = computed(() => data.value?.kpis || {})
const scopeList = computed(() => data.value?.scopes || [])
const scopeLabel = computed(() => ({
  ALL: '全部支出', DAILY: '日常支出', FIXED: '固定支出', SPECIAL: '专项支出'
}[scope.value]))
const budgetItems = computed(() => [...(data.value?.budget?.items || [])]
  .sort((a, b) => Number(b.pct) - Number(a.pct)))
const budgetTone = computed(() => Number(data.value?.budget?.pct) > 100 ? 'bad'
  : Number(data.value?.budget?.pct) > 90 ? 'warn' : 'ok')
const annualTone = computed(() => {
  const a = data.value?.annualBudget
  if (!a || !a.total) return 'ok'
  if (Number(a.pct) > 100) return 'bad'
  return Number(a.pct) > Number(a.timePct) + 5 ? 'warn' : 'ok'
})

const nf0 = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 })
const nf2 = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
function money(n, dec = 0) {
  const v = Number(n) || 0
  return dec === 2 ? nf2.format(v) : nf0.format(v)
}
function signedMoney(n, dec = 0) {
  const v = Number(n) || 0
  return (v < 0 ? '-¥' : '¥') + money(Math.abs(v), dec)
}
function scopeAmount(s) {
  return Number(scopeList.value.find(x => x.scope === s)?.amount || 0)
}
function scopeColor(s) {
  return COLORS[s] || '#98a2b3'
}
function scopeShort(s) {
  return ({ ALL: '全部', DAILY: '日常', FIXED: '固定', SPECIAL: '专项' })[s] || s
}
function setScope(s) {
  if (scope.value === s) return
  scope.value = s
  reload()
}

function deltaInfo(cur, prev, upIsGood, pp = false) {
  if (prev == null || cur == null) return { text: '—', tone: 'flat', note: '' }
  const c = Number(cur)
  const p = Number(prev)
  if (pp && p < 0) {
    if (c >= 0) return { text: '转正', tone: 'good', note: '较上期' }
    return { text: '—', tone: 'flat', note: '' }
  }
  if (pp && p > 0 && c < 0) return { text: '转负', tone: 'bad', note: '较上期' }
  if (!pp && p < 0) {
    if (c > 0) return { text: '扭亏', tone: 'good', note: '较上期' }
    if (c < 0) return { text: '—', tone: 'flat', note: '' }
  }
  if (!pp && p > 0 && c < 0) return { text: '转亏', tone: 'bad', note: '较上期' }
  if (p === 0) return { text: '—', tone: 'flat', note: '' }
  const d = c - p
  if (Math.abs(d) < 0.005) return { text: '持平', tone: 'flat', note: '较上期' }
  const tone = (upIsGood ? d > 0 : d < 0) ? 'good' : 'bad'
  const text = pp
    ? `${d > 0 ? '↑' : '↓'} ${Math.abs(d).toFixed(1)}pp`
    : `${d > 0 ? '↑' : '↓'} ${Math.abs(d / p * 100).toFixed(1)}%`
  return { text, tone, note: '较上期' }
}

const heroDelta = computed(() => deltaInfo(kpis.value.expense, kpis.value.prevExpense, false))
const stats = computed(() => [
  { label: '收入', value: `¥${money(kpis.value.income)}`, delta: deltaInfo(kpis.value.income, kpis.value.prevIncome, true) },
  { label: '结余', value: signedMoney(kpis.value.net), delta: deltaInfo(kpis.value.net, kpis.value.prevNet, true) },
  { label: '储蓄率', value: `${Number(kpis.value.savingsRate || 0).toFixed(1)}%`, delta: deltaInfo(kpis.value.savingsRate, kpis.value.prevSavingsRate, true, true) },
  { label: '日均支出', value: `¥${money(kpis.value.avgDaily)}`, delta: deltaInfo(kpis.value.avgDaily, kpis.value.prevAvgDaily, false) }
])

const calendar = computed(() => {
  const empty = { cells: [], active: 0, avg: 0, top: 0, topDay: 0, top3Share: 0 }
  if (!data.value) return empty
  const [y, m] = month.value.split('-').map(Number)
  const days = new Date(y, m, 0).getDate()
  const lead = (new Date(y, m - 1, 1).getDay() + 6) % 7
  const byDate = new Map((data.value.daily || []).map(d => [d.date, d]))
  const cells = []
  for (let i = 0; i < lead; i++) cells.push({ empty: true })
  const vals = []
  for (let d = 1; d <= days; d++) {
    const key = `${month.value}-${String(d).padStart(2, '0')}`
    const item = byDate.get(key)
    const v = item
      ? Number(scope.value === 'ALL' ? item.expense : item[scope.value.toLowerCase()] || 0)
      : 0
    vals.push(v)
    cells.push({ day: d, value: v, date: key })
  }
  const max = Math.max(...vals, 1)
  for (const c of cells) {
    if (c.empty) continue
    c.level = c.value <= 0 ? 0
      : c.value / max > 0.66 ? 5
        : c.value / max > 0.33 ? 4
          : c.value / max > 0.15 ? 3
            : c.value / max > 0.05 ? 2 : 1
    c.display = c.value > 0 ? money(c.value) : ''
  }
  const active = vals.filter(v => v > 0)
  const sum = active.reduce((a, b) => a + b, 0)
  const top = Math.max(...vals)
  return {
    cells,
    active: active.length,
    avg: active.length ? sum / active.length : 0,
    top,
    topDay: vals.indexOf(top) + 1,
    top3Share: sum ? [...vals].sort((a, b) => b - a).slice(0, 3).reduce((a, b) => a + b, 0) / sum * 100 : 0
  }
})

const nwRows = computed(() => {
  const items = data.value?.accounts?.netWorthTrend || []
  return items.map((x, i) => ({
    ...x,
    change: i === 0 ? null : Number(x.netWorth) - Number(items[i - 1].netWorth)
  }))
})
const maxRankAmount = computed(() => Math.max(...(data.value?.categories || []).map(c => Number(c.amount)), 1))
const maxMerchantAmount = computed(() => Math.max(...(data.value?.merchants || []).map(m => Number(m.amount)), 1))

function rankWidth(c) {
  return `${(Number(c.amount) / maxRankAmount.value) * 100}%`
}
function merchantWidth(m) {
  return `${(Number(m.amount) / maxMerchantAmount.value) * 100}%`
}
function rankDelta(c) {
  if (c.prevAmount == null) return { text: '—', tone: 'flat' }
  if (Number(c.prevAmount) === 0) return { text: '新增', tone: 'up' }
  if (c.deltaPct == null) return { text: '—', tone: 'flat' }
  const p = Number(c.deltaPct)
  if (Math.abs(p) < 0.5) return { text: '持平', tone: 'flat' }
  if (Math.abs(p) > 999) {
    const ratio = Number(c.amount) / Number(c.prevAmount)
    return { text: `${p > 0 ? '↑' : '↓'}${Math.abs(ratio).toFixed(0)}×`, tone: p > 0 ? 'up' : 'down' }
  }
  return { text: `${p > 0 ? '↑' : '↓'}${Math.abs(p).toFixed(0)}%`, tone: p > 0 ? 'up' : 'down' }
}
function drawerChildWidth(c) {
  const max = Math.max(...(drawer.value.category?.children || []).map(x => Number(x.amount)), 1)
  return `${(Number(c.amount) / max) * 100}%`
}

function reload() {
  const seq = ++requestSeq
  loading.value = true
  router.replace({
    query: { ...route.query, month: month.value, scope: scope.value, level: level.value, compare: compare.value }
  })
  reportApi.overview({
    ...range.value,
    compare: compare.value,
    scope: scope.value,
    level: level.value
  }).then(res => {
    if (seq !== requestSeq) return
    data.value = res.data
    nextTick(() => {
      renderTrend()
      renderNetWorth()
    })
  }).catch(e => {
    if (seq === requestSeq) message.error(e.message || '加载报表失败')
  }).finally(() => {
    if (seq === requestSeq) loading.value = false
  })
}

function openCategory(c) {
  drawer.value = { show: true, loading: true, category: c, items: [] }
  reportApi.categoryDetail({
    ...range.value,
    categoryId: c.categoryId,
    scope: scope.value
  }).then(res => {
    drawer.value.items = res.data?.categoryDetails || []
  }).catch(() => {
    message.error('加载分类明细失败')
  }).finally(() => {
    drawer.value.loading = false
  })
}

function axisFmt(v, useWan = false) {
  if (!v) return '0'
  if (useWan) return `${(v / 10000).toFixed(v % 10000 === 0 ? 0 : 1)}万`
  return nf0.format(v)
}

function renderTrend() {
  if (!trendEl.value || !data.value) return
  const trend = data.value.trend || []
  const xs = trend.map(t => `${Number(t.month.slice(5))}月`)
  const selectedIdx = trend.findIndex(t => t.month === month.value)
  const key = scope.value === 'ALL' ? null : scope.value.toLowerCase()
  const seriesVals = trend.map(t => scope.value === 'ALL'
    ? Number(t.expense || 0) : Number(t[key] || 0))
  const budgetTotal = data.value.budget?.exists ? Number(data.value.budget.total || 0) : 0
  const useWan = Math.max(...seriesVals, budgetTotal, 0) >= 10000
  const base = {
    grid: { left: 6, right: 16, top: scope.value === 'ALL' ? 34 : 22, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const idx = params[0]?.dataIndex ?? 0
        const t = trend[idx]
        if (!t) return ''
        const rows = scope.value === 'ALL'
          ? [
              ['日常', t.daily], ['固定', t.fixed], ['专项', t.special], ['合计', t.expense]
            ]
          : [[scopeLabel.value, t[scope.value.toLowerCase()] || 0]]
        return `<b>${t.month}</b><br>` + rows
          .map(([name, v]) => `${name} ¥${money(v, 2)}`).join('<br>')
      }
    },
    legend: scope.value === 'ALL'
      ? { top: 0, right: 0, itemWidth: 9, itemHeight: 9, textStyle: { color: '#64748b', fontSize: 11 } }
      : undefined,
    xAxis: {
      type: 'category',
      data: xs,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: {
        color: (v, i) => i === selectedIdx ? '#0f172a' : '#94a3b8',
        fontWeight: (v, i) => i === selectedIdx ? 700 : 400,
        fontSize: props.mobile ? 10 : 11
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#94a3b8', fontSize: 10, formatter: v => axisFmt(v, useWan) },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    series: []
  }

  if (scope.value === 'ALL') {
    const conf = [
      { name: '日常', key: 'daily', color: COLORS.DAILY },
      { name: '固定', key: 'fixed', color: COLORS.FIXED },
      { name: '专项', key: 'special', color: COLORS.SPECIAL }
    ]
    base.series = conf.map((c, i) => ({
      name: c.name,
      type: 'bar',
      stack: 'total',
      barMaxWidth: props.mobile ? 13 : 20,
      data: trend.map(t => Number(t[c.key] || 0)),
      itemStyle: {
        color: c.color,
        borderColor: '#fff',
        borderWidth: 1.5,
        borderRadius: i === conf.length - 1 ? [4, 4, 0, 0] : 0
      }
    }))
  } else {
    const budget = data.value.budget?.exists ? Number(data.value.budget.total) : null
    const series = {
      name: scopeLabel.value,
      type: 'bar',
      barMaxWidth: props.mobile ? 14 : 22,
      data: trend.map(t => {
        const v = Number(t[key] || 0)
        const selected = t.month === month.value
        const over = budget != null && v > budget
        return {
          value: v,
          itemStyle: {
            color: selected ? (over ? '#d03b3b' : '#2a78d6') : '#9ec5f4',
            borderRadius: [4, 4, 0, 0]
          }
        }
      }),
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      markPoint: {
        symbolSize: 0,
        label: { show: true, position: 'top', color: '#475467', fontSize: 10, formatter: p => money(p.value) },
        data: [{ type: 'max' }]
      }
    }
    if (scope.value === 'DAILY' && data.value.budget?.exists) {
      series.markLine = {
        silent: true,
        symbol: 'none',
        lineStyle: { type: 'dashed', color: '#94a3b8', width: 1.5 },
        label: { formatter: `预算 ${money(data.value.budget.total)}`, position: 'insideEndTop', color: '#64748b', fontSize: 10 },
        data: [{ yAxis: Number(data.value.budget.total) }]
      }
    }
    base.series = [series]
  }

  if (!trendChart) trendChart = echarts.init(trendEl.value)
  trendChart.setOption(base, true)
}

function renderNetWorth() {
  if (!netWorthEl.value || !data.value) return
  const items = data.value.accounts?.netWorthTrend || []
  if (items.length < 2) {
    if (netWorthChart) { netWorthChart.dispose(); netWorthChart = null }
    return
  }
  if (!netWorthChart) netWorthChart = echarts.init(netWorthEl.value)
  netWorthChart.setOption({
    grid: { left: 6, right: 10, top: 12, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      formatter: params => `${params[0]?.name}<br>净资产 ¥${money(params[0]?.value, 0)}`
    },
    xAxis: {
      type: 'category',
      data: items.map(i => `${Number(i.month.slice(5))}月`),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#94a3b8', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLabel: { color: '#94a3b8', fontSize: 10, formatter: v => axisFmt(v, true) },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    series: [{
      type: 'line',
      data: items.map(i => Number(i.netWorth || 0)),
      symbol: 'circle',
      symbolSize: 5,
      smooth: false,
      lineStyle: { width: 2, color: '#0ea5a4' },
      itemStyle: { color: '#0ea5a4', borderColor: '#fff', borderWidth: 2 },
      areaStyle: { color: 'rgba(14,165,164,0.10)' }
    }]
  }, true)
}

function resizeCharts() {
  trendChart?.resize()
  netWorthChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
  reload()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  netWorthChart?.dispose()
})
</script>

<style scoped>
.report-page { color: var(--bp-ink); }
.rp-head {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: 16px; flex-wrap: wrap; margin-bottom: 16px;
}
.rp-head h2 { margin: 0; font-size: 24px; letter-spacing: -0.02em; }
.rp-title p { margin: 3px 0 0; font-size: 12px; color: var(--bp-muted); }
.rp-filters { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.rp-alert { margin-bottom: 12px; }

.rp-body { transition: opacity 0.15s; }
.rp-body.dim { opacity: 0.55; pointer-events: none; }
.rp-card {
  background: var(--bp-surface);
  border: 1px solid var(--bp-line);
  border-radius: 14px;
  padding: 18px;
  box-shadow: var(--bp-shadow);
  min-width: 0;
}
.rp-card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.rp-card-head h3 { margin: 0; font-size: 15px; letter-spacing: -0.01em; }
.rp-cap { margin: 2px 0 0; font-size: 12px; color: var(--bp-muted); line-height: 1.55; }
.rp-muted { color: var(--bp-muted); font-size: 12px; }
.rp-link {
  border: 0; background: transparent; color: var(--bp-primary);
  font-size: 12px; cursor: pointer; padding: 4px 0; white-space: nowrap;
}
.rp-link:hover { text-decoration: underline; }

.rp-grid { display: grid; gap: 16px; margin-bottom: 16px; }
.rp-grid-8-4 { grid-template-columns: minmax(0, 2fr) minmax(0, 1fr); }
.rp-grid-7-5 { grid-template-columns: minmax(0, 7fr) minmax(0, 5fr); }
.rp-hero-grid {
  display: grid; grid-template-columns: minmax(0, 1.62fr) minmax(0, 1fr);
  gap: 16px; margin-bottom: 16px;
}

.rp-eyebrow { font-size: 11px; font-weight: 700; letter-spacing: 0.12em; color: var(--bp-muted); }
.rp-hero-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; }
.rp-figure {
  margin-top: 6px; font-size: 44px; line-height: 1.08; font-weight: 750; letter-spacing: -0.03em;
}
.rp-figure .cur { font-size: 24px; font-weight: 600; letter-spacing: 0; color: var(--bp-muted); margin-right: 2px; }
.rp-delta-line { display: flex; align-items: center; gap: 8px; margin-top: 8px; min-height: 20px; flex-wrap: wrap; }
.rp-hero-total { text-align: right; font-size: 12px; color: var(--bp-muted); padding-top: 4px; }
.rp-hero-total strong { display: block; font-size: 18px; color: var(--bp-ink); margin-top: 2px; }
.rp-delta-chip {
  display: inline-flex; align-items: center; border-radius: 999px;
  padding: 2px 8px; font-size: 11px; font-weight: 650; white-space: nowrap;
  background: #f1f5f9; color: #64748b;
}
.rp-delta-chip.good { background: #ecfdf3; color: #067647; }
.rp-delta-chip.bad { background: #fef3f2; color: #b42318; }
.rp-delta-chip.flat { background: #f1f5f9; color: #64748b; }

.rp-segbar { display: flex; gap: 2px; height: 26px; margin-top: 18px; border-radius: 6px; overflow: hidden; }
.rp-segbar button {
  border: 0; padding: 0 0 0 9px; cursor: pointer; display: flex; align-items: center;
  color: #fff; font-size: 11px; font-weight: 700; min-width: 0; overflow: hidden; white-space: nowrap;
}
.rp-segbar button:focus-visible { outline: 2px solid #0f172a; outline-offset: -2px; }
.rp-seglegend { display: flex; align-items: center; gap: 16px; margin-top: 10px; flex-wrap: wrap; }
.rp-leg {
  display: flex; align-items: center; gap: 6px; border: 0; background: transparent;
  padding: 2px 0; font-size: 12px; color: #64748b; cursor: pointer;
}
.rp-leg i { width: 8px; height: 8px; border-radius: 3px; display: block; }
.rp-leg b { color: var(--bp-ink); font-weight: 650; }
.rp-leg.sel b { text-decoration: underline; text-underline-offset: 3px; }
.rp-seg-total { margin-left: auto; font-size: 12px; color: var(--bp-muted); white-space: nowrap; }
.rp-seg-total strong { color: var(--bp-ink); }

.rp-stats { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.rp-stat {
  background: var(--bp-surface); border: 1px solid var(--bp-line); border-radius: 14px;
  padding: 15px 16px 13px; box-shadow: var(--bp-shadow);
  display: flex; flex-direction: column; justify-content: space-between; min-height: 104px;
}
.rp-stat-label { font-size: 12px; color: var(--bp-muted); }
.rp-stat-value { margin-top: 4px; font-size: 22px; font-weight: 730; letter-spacing: -0.02em; }
.rp-stat-foot { display: flex; align-items: center; gap: 6px; margin-top: 8px; flex-wrap: wrap; }

.rp-chart { width: 100%; }
.rp-table { width: 100%; border-collapse: collapse; margin-top: 12px; font-size: 12px; }
.rp-table th, .rp-table td { padding: 7px 8px; border-bottom: 1px solid var(--bp-line); text-align: left; white-space: nowrap; }
.rp-table th { color: var(--bp-muted); font-weight: 600; font-size: 11px; background: #fafbfc; }
.rp-table .n { text-align: right; font-variant-numeric: tabular-nums; }
.rp-table tr.sel td { background: #f0f6fe; font-weight: 600; }

.rp-budget-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 10px; margin-top: 14px; }
.rp-budget-value { font-size: 24px; font-weight: 750; letter-spacing: -0.02em; }
.rp-budget-value small { font-size: 12px; font-weight: 500; color: var(--bp-muted); letter-spacing: 0; }
.rp-pct {
  font-size: 12px; font-weight: 700; border-radius: 999px; padding: 3px 9px;
  background: #fef3f2; color: #b42318; white-space: nowrap;
}
.rp-pct.ok { background: #ecfdf3; color: #067647; }
.rp-pct.warn { background: #fffaeb; color: #b54708; }
.rp-meter {
  position: relative; height: 10px; border-radius: 999px; background: #eef2f6; margin-top: 10px;
}
.rp-meter > span {
  display: block; height: 100%; border-radius: 999px; background: #2a78d6; min-width: 2px;
}
.rp-meter > span.bad { background: #d03b3b; }
.rp-meter > span.warn { background: #f79009; }
.rp-meter > span.ok { background: #2a78d6; }
.rp-time-mark {
  position: absolute; top: -4px; bottom: -4px; width: 2px; background: #0f172a; border-radius: 2px;
}
.rp-budget-list { margin-top: 14px; display: flex; flex-direction: column; gap: 10px; }
.rp-brow { display: grid; grid-template-columns: 64px minmax(0, 1fr) 52px; gap: 10px; align-items: center; font-size: 12px; }
.rp-brow-name { color: #475467; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rp-brow-track { height: 7px; border-radius: 999px; background: #eef2f6; overflow: hidden; }
.rp-brow-track span { display: block; height: 100%; border-radius: 999px; background: #2a78d6; }
.rp-brow-track span.bad { background: #d03b3b; }
.rp-brow-track span.warn { background: #f79009; }
.rp-brow-val { text-align: right; font-variant-numeric: tabular-nums; color: var(--bp-muted); }
.rp-brow-val b { color: var(--bp-ink); }
.rp-budget-more { margin-top: 10px; font-size: 11px; }
.rp-good { color: #067647; }
.rp-bad { color: #b42318; }

.rp-rank-head, .rp-rank-row {
  display: grid;
  grid-template-columns: 22px 10px minmax(96px, 1.05fr) minmax(80px, 1.6fr) 92px 52px 58px;
  gap: 10px; align-items: center;
}
.rp-rank-head { padding: 14px 4px 6px; font-size: 11px; color: var(--bp-muted); border-bottom: 1px solid var(--bp-line); }
.rp-rank-head .n { text-align: right; }
.rp-rank-row {
  width: 100%; border: 0; border-bottom: 1px solid #f2f4f7; background: transparent;
  padding: 9px 4px; text-align: left; cursor: pointer; border-radius: 6px;
}
.rp-rank-row:hover { background: #f8fafc; }
.rp-rank-row:focus-visible { outline: 2px solid var(--bp-primary); outline-offset: -2px; }
.rp-rank-no { font-size: 11px; color: var(--bp-muted); font-variant-numeric: tabular-nums; }
.rp-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; }
.rp-rank-name { min-width: 0; }
.rp-rank-name .nm { display: block; font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rp-rank-name .meta { display: flex; gap: 6px; align-items: center; font-size: 11px; color: var(--bp-muted); margin-top: 1px; white-space: nowrap; overflow: hidden; }
.rp-budget-chip { font-size: 10px; padding: 0 5px; line-height: 16px; border-radius: 4px; background: #f1f5f9; color: #475467; }
.rp-budget-chip.bad { background: #fef3f2; color: #b42318; }
.rp-budget-chip.warn { background: #fffaeb; color: #b54708; }
.rp-rank-bar { height: 8px; background: #f2f4f7; border-radius: 4px; overflow: hidden; }
.rp-rank-bar span { display: block; height: 100%; background: #2a78d6; border-radius: 0 4px 4px 0; }
.rp-rank-amt { text-align: right; font-size: 13px; font-weight: 650; font-variant-numeric: tabular-nums; white-space: nowrap; }
.rp-rank-share { text-align: right; font-size: 12px; color: var(--bp-muted); font-variant-numeric: tabular-nums; }
.rp-rank-delta { text-align: right; font-size: 11px; font-variant-numeric: tabular-nums; white-space: nowrap; }
.rp-rank-delta.up { color: #b42318; font-weight: 650; }
.rp-rank-delta.down { color: #067647; font-weight: 650; }
.rp-rank-delta.flat { color: var(--bp-muted); }

.rp-heat-legend { display: flex; align-items: center; gap: 4px; font-size: 10px; color: var(--bp-muted); }
.rp-heat-legend i { display: block; width: 13px; height: 8px; border-radius: 2px; }
.rp-heat-legend .h1 { background: #eaf2fd; }
.rp-heat-legend .h2 { background: #cde2fb; }
.rp-heat-legend .h3 { background: #86b6ef; }
.rp-heat-legend .h4 { background: #2a78d6; }
.rp-heat-legend .h5 { background: #184f95; }
.rp-cal { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; margin-top: 12px; }
.rp-cal-wd { text-align: center; font-size: 10px; color: var(--bp-muted); padding-bottom: 2px; }
.rp-cal-cell {
  height: 54px; border-radius: 7px; background: #f8fafc; padding: 5px 6px;
  display: flex; flex-direction: column; justify-content: space-between;
  font-size: 10px; color: var(--bp-muted); min-width: 0; overflow: hidden;
}
.rp-cal-cell .d { font-variant-numeric: tabular-nums; }
.rp-cal-cell .a { font-size: 11px; font-weight: 650; font-variant-numeric: tabular-nums; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rp-cal-cell.empty { background: transparent; }
.rp-cal-cell.l1 { background: #eaf2fd; color: #334155; }
.rp-cal-cell.l2 { background: #cde2fb; color: #1e3a5f; }
.rp-cal-cell.l3 { background: #86b6ef; color: #0b2b50; }
.rp-cal-cell.l4 { background: #2a78d6; color: #fff; }
.rp-cal-cell.l5 { background: #184f95; color: #fff; }
.rp-cal-foot {
  display: flex; gap: 14px; flex-wrap: wrap; margin-top: 12px; padding-top: 10px;
  border-top: 1px solid var(--bp-line); font-size: 11px; color: var(--bp-muted);
}
.rp-cal-foot b { color: var(--bp-ink); font-variant-numeric: tabular-nums; }

.rp-merchant-row {
  display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(60px, 1fr) 88px;
  gap: 10px; align-items: center; padding: 10px 2px; border-bottom: 1px solid #f2f4f7;
}
.rp-merchant-row:last-child { border-bottom: 0; }
.rp-merchant-name { font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rp-merchant-meta { display: flex; gap: 6px; align-items: center; margin-top: 1px; font-size: 11px; color: var(--bp-muted); white-space: nowrap; overflow: hidden; }
.rp-badge { display: inline-block; font-size: 10px; line-height: 16px; padding: 0 5px; border-radius: 4px; background: #f1f5f9; color: #475467; }
.rp-badge.hot { background: #fff4e5; color: #b54708; }
.rp-merchant-bar { height: 7px; background: #f2f4f7; border-radius: 4px; overflow: hidden; }
.rp-merchant-bar span { display: block; height: 100%; background: #62a0e8; border-radius: 0 4px 4px 0; }
.rp-merchant-amt { text-align: right; font-size: 12px; font-weight: 650; font-variant-numeric: tabular-nums; white-space: nowrap; }

.rp-annual-total { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; margin-top: 14px; }
.rp-annual-list { margin-top: 14px; display: flex; flex-direction: column; gap: 9px; }
.rp-arow { display: grid; grid-template-columns: 74px minmax(0, 1fr) 46px; gap: 10px; align-items: center; font-size: 12px; }
.rp-arow-name { display: flex; align-items: center; gap: 5px; color: #475467; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rp-arow-track { height: 6px; border-radius: 999px; background: #eef2f6; overflow: hidden; }
.rp-arow-track span { display: block; height: 100%; border-radius: 999px; background: #2a78d6; }
.rp-arow-track span.done { background: #1baf7a; }
.rp-arow-track span.warn { background: #f79009; }
.rp-arow-val { text-align: right; color: var(--bp-muted); font-variant-numeric: tabular-nums; font-size: 11px; }
.rp-check {
  width: 13px; height: 13px; border-radius: 50%; background: #e6f7f0; color: #0e8a5f;
  display: inline-grid; place-items: center; font-size: 9px; font-weight: 800; font-style: normal; flex: 0 0 auto;
}

.rp-accounts { margin-bottom: 16px; }
.rp-acct-top { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr); gap: 24px; align-items: end; margin-top: 6px; }
.rp-networth { font-size: 30px; font-weight: 750; letter-spacing: -0.02em; margin-top: 3px; }
.rp-split-legend { display: flex; gap: 14px; flex-wrap: wrap; margin-top: 6px; font-size: 11px; color: var(--bp-muted); }
.rp-split-legend b { color: var(--bp-ink); font-variant-numeric: tabular-nums; }
.rp-acct-split { display: flex; gap: 2px; height: 14px; border-radius: 4px; overflow: hidden; }
.rp-acct-split span { display: block; height: 100%; }
.rp-acct-groups { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-top: 16px; }
.rp-acct-group { background: #fafbfc; border: 1px solid var(--bp-line); border-radius: 10px; padding: 10px 12px; }
.rp-acct-group .v { margin-top: 3px; font-size: 15px; font-weight: 700; font-variant-numeric: tabular-nums; }
.rp-acct-group .v.neg { color: #b42318; }
.rp-acct-body { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr); gap: 24px; margin-top: 14px; }
.rp-acct-list { display: grid; grid-template-columns: 1fr 1fr; gap: 0 24px; }
.rp-acct-row {
  display: grid; grid-template-columns: minmax(0, 1fr) 96px; gap: 10px;
  padding: 7px 0; border-bottom: 1px solid #f2f4f7; font-size: 12px; align-items: center;
}
.rp-acct-row .nm { color: #475467; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rp-acct-row .amt { text-align: right; font-variant-numeric: tabular-nums; font-weight: 600; }
.rp-acct-row .amt.neg { color: #b42318; }

.rp-insights { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-bottom: 16px; }
.rp-insight {
  background: var(--bp-surface); border: 1px solid var(--bp-line); border-radius: 14px;
  padding: 15px; box-shadow: var(--bp-shadow); display: flex; gap: 11px;
}
.rp-insight-icon {
  width: 32px; height: 32px; border-radius: 10px; display: grid; place-items: center;
  flex: 0 0 auto; background: #eff6ff; color: #2563eb;
}
.rp-insight-icon.bad { background: #fef3f2; color: #b42318; }
.rp-insight-icon.warn { background: #fffaeb; color: #b54708; }
.rp-insight-icon.good { background: #ecfdf3; color: #067647; }
.rp-insight h4 { margin: 0; font-size: 13px; }
.rp-insight p { margin: 4px 0 0; font-size: 12px; line-height: 1.65; color: #475467; }
.rp-foot { font-size: 11px; line-height: 1.7; color: var(--bp-muted); margin: 0 0 8px; }

.rp-drawer-total { font-size: 28px; font-weight: 750; letter-spacing: -0.02em; }
.rp-drawer-sec { margin-top: 20px; }
.rp-drawer-sec h4 { margin: 0 0 8px; font-size: 12px; color: var(--bp-muted); font-weight: 600; }
.rp-drawer-row { padding: 7px 0; border-bottom: 1px solid #f2f4f7; font-size: 12px; }
.rp-drawer-row .nm { display: flex; justify-content: space-between; gap: 8px; }
.rp-drawer-row .nm span:last-child { font-variant-numeric: tabular-nums; font-weight: 600; }
.rp-drawer-bar { height: 5px; background: #f2f4f7; border-radius: 3px; overflow: hidden; margin-top: 5px; }
.rp-drawer-bar span { display: block; height: 100%; background: #62a0e8; border-radius: 0 3px 3px 0; }
.rp-drawer-tx {
  display: flex; justify-content: space-between; gap: 12px;
  padding: 8px 0; border-bottom: 1px solid #f2f4f7;
}
.rp-drawer-tx-note { font-size: 12px; color: #334155; }
.rp-drawer-tx-amt { font-size: 12px; font-weight: 650; font-variant-numeric: tabular-nums; white-space: nowrap; color: #b42318; }
.rp-drawer-tx-amt.refund { color: #067647; }
.rp-drawer-note { margin-top: 18px; padding: 10px 12px; background: #f8fafc; border-radius: 8px; font-size: 11px; line-height: 1.65; color: var(--bp-muted); }

.mobile .rp-grid-8-4,
.mobile .rp-grid-7-5,
.mobile .rp-hero-grid,
.mobile .rp-acct-top,
.mobile .rp-acct-body,
.mobile .rp-insights { grid-template-columns: 1fr; }
.mobile .rp-head { align-items: flex-start; }
.mobile .rp-filters { width: 100%; }
.report-page.mobile { padding-bottom: calc(84px + env(safe-area-inset-bottom, 0px)); }
.mobile .rp-title h2 { display: none; }
.mobile .rp-figure { font-size: 34px; }
.mobile .rp-hero-top { flex-direction: column; gap: 6px; }
.mobile .rp-hero-total { text-align: left; }
.mobile .rp-seglegend { gap: 8px; }
.mobile .rp-seg-total { margin-left: 0; }
.mobile .rp-rank-head,
.mobile .rp-rank-row { grid-template-columns: 18px 8px minmax(80px, 1.2fr) minmax(50px, 1fr) 80px 50px; }
.mobile .rp-rank-head > :nth-child(6),
.mobile .rp-rank-share { display: none; }
.mobile .rp-rank-name .meta { white-space: normal; flex-wrap: wrap; row-gap: 2px; }
.mobile .rp-acct-groups { grid-template-columns: 1fr 1fr; }
.mobile .rp-acct-list { grid-template-columns: 1fr; }
.mobile .rp-cal-cell { height: 48px; padding: 4px; }
.mobile .rp-cal-cell .a { font-size: 10px; }
.mobile .rp-stat-value { font-size: 19px; }
.mobile .rp-networth { font-size: 26px; }

@media (max-width: 1100px) {
  .rp-grid-8-4, .rp-grid-7-5, .rp-hero-grid, .rp-acct-top, .rp-acct-body { grid-template-columns: 1fr; }
  .rp-insights { grid-template-columns: 1fr; }
  .rp-acct-groups { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .rp-head h2 { font-size: 22px; }
  .rp-figure { font-size: 34px; }
  .rp-hero-top { flex-direction: column; gap: 6px; }
  .rp-hero-total { text-align: left; }
  .rp-stats { grid-template-columns: 1fr 1fr; }
  .rp-stat-value { font-size: 19px; }
  .rp-rank-head, .rp-rank-row { grid-template-columns: 18px 8px minmax(80px, 1.2fr) minmax(50px, 1fr) 80px 50px; }
  .rp-rank-head > :nth-child(6), .rp-rank-share { display: none; }
  .rp-rank-name .meta { white-space: normal; flex-wrap: wrap; row-gap: 2px; }
  .rp-cal-cell { height: 48px; padding: 4px; }
  .rp-cal-cell .a { font-size: 9px; }
  .rp-acct-groups { grid-template-columns: 1fr 1fr; }
  .rp-acct-list { grid-template-columns: 1fr; }
  .rp-networth { font-size: 26px; }
  .rp-card { padding: 15px; }
}
</style>