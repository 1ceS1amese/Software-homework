<template>
  <div class="grid min-h-dvh items-start justify-items-center bg-(--csms-canvas) px-4 py-6 sm:px-6 sm:py-4 md:items-center">
    <main class="auth-frame grid w-full max-w-270 overflow-hidden rounded-[1.5rem] border border-(--csms-line) bg-(--csms-paper) shadow-lg shadow-slate-900/5 md:grid-cols-[0.78fr_1.22fr]">
      <section class="flex min-w-0 flex-col justify-between bg-(--csms-tint) px-7 py-6 sm:px-9 sm:py-8 md:p-10" aria-label="平台介绍">
        <div class="flex items-center gap-3">
          <div class="grid size-11 shrink-0 place-items-center rounded-xl bg-(--csms-accent) text-white">
            <svg class="size-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="m2 10 10-5 10 5-10 5-10-5Z" />
              <path d="M6 12v5c3.6 2.7 8.4 2.7 12 0v-5M22 10v6" />
            </svg>
          </div>
          <div class="min-w-0">
            <div class="text-base font-semibold text-(--csms-ink)">CSMS 教务平台</div>
            <div class="text-xs text-(--csms-muted)">课程与教学管理</div>
          </div>
        </div>
        <div class="hidden md:block">
          <p class="mb-5 text-xs font-medium text-(--csms-accent)">清晰管理每一门课</p>
          <h1 class="max-w-80 text-2xl leading-[1.35] font-semibold text-(--csms-ink) text-balance lg:text-3xl">让教务工作，<br />更有条理。</h1>
          <p class="mt-5 max-w-75 text-sm leading-7 text-(--csms-muted) text-pretty">开课、选课与成绩，都在这里。</p>
        </div>
        <div class="hidden border-t border-(--csms-line) pt-5 text-xs text-(--csms-muted) md:block">开课　·　选课　·　成绩</div>
      </section>
      <section class="flex min-w-0 items-center px-7 py-8 sm:px-10 md:px-12" aria-label="账户登录">
        <div class="mx-auto w-full max-w-105"><slot /></div>
      </section>
    </main>
  </div>
</template>
