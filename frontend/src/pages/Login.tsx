import React, { useState } from "react";

import AppLogo from "../components/AppLogo";

type LoginPayload = {
  id: string;
  password: string;
};

type SignupPayload = {
  username: string;
  email: string;
  password: string;
  fullName: string;
};

type LoginProps = {
  onLogin?: (payload: LoginPayload) => void | Promise<void>;
  onSignup?: (payload: SignupPayload) => void | Promise<void>;
  onLocalTestLogin?: () => void;
};

type AuthMode = "login" | "signup";

const usernamePattern = /^(?=.*[a-z])[a-z0-9_]+$/;
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).+$/;
const fullNamePattern = /^[A-Za-z가-힣][A-Za-z가-힣\s.'-]{1,49}$/;

function validateSignup(payload: SignupPayload): string | null {
  if (!usernamePattern.test(payload.username)) {
    return "아이디는 영문 소문자, 숫자, 밑줄만 사용할 수 있습니다.";
  }

  if (!emailPattern.test(payload.email)) {
    return "올바른 이메일 주소를 입력하세요.";
  }

  if (!passwordPattern.test(payload.password)) {
    return "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.";
  }

  if (!fullNamePattern.test(payload.fullName) || !/[A-Za-z가-힣]{2,}/.test(payload.fullName.replace(/[\s.'-]/g, ""))) {
    return "이름은 한글 또는 영문 2자 이상으로 입력하세요.";
  }

  return null;
}

export default function Login({ onLogin, onSignup, onLocalTestLogin }: LoginProps) {
  const [mode, setMode] = useState<AuthMode>("login");
  const [id, setId] = useState("");
  const [email, setEmail] = useState("");
  const [fullName, setFullName] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isSignup = mode === "signup";

  const switchMode = (nextMode: AuthMode) => {
    setMode(nextMode);
    setPassword("");
    setShowPassword(false);
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const username = id.trim();

    if (isSignup) {
      const payload = {
        username,
        email: email.trim(),
        password,
        fullName: fullName.trim(),
      };
      const validationError = validateSignup(payload);

      if (validationError) {
        alert(validationError);
        return;
      }

      try {
        setIsSubmitting(true);
        await onSignup?.(payload);
        alert("계정이 생성되었습니다. 로그인해 주세요.");
        setMode("login");
        setEmail("");
        setFullName("");
        setPassword("");
      } finally {
        setIsSubmitting(false);
      }

      return;
    }

    if (!username || !password.trim()) {
      alert("아이디와 비밀번호를 입력하세요.");
      return;
    }

    try {
      setIsSubmitting(true);
      await onLogin?.({
        id: username,
        password,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#0B0F1A] px-6 py-10 font-sans text-slate-200 selection:bg-indigo-500/30">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <video
          className="h-full w-full object-cover opacity-70"
          src="/login-background.mp4"
          autoPlay
          muted
          loop
          playsInline
          aria-hidden="true"
        />
        <div className="absolute inset-0 bg-[#050816]/55" />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_30%,rgba(37,99,235,0.18),transparent_42%),linear-gradient(90deg,rgba(2,6,23,0.75),rgba(2,6,23,0.35),rgba(2,6,23,0.8))]" />
      </div>

      <div className="relative z-10 grid w-full max-w-5xl grid-cols-1 overflow-hidden rounded-[32px] border border-slate-800/90 bg-[#0D1117]/88 shadow-2xl shadow-black/50 backdrop-blur-md lg:grid-cols-[1.1fr_0.9fr]">
        <section className="hidden min-h-[680px] flex-col border-r border-slate-800/90 bg-gradient-to-br from-[#111827]/92 via-[#0F172A]/88 to-[#0D1117]/92 p-10 lg:flex">
          <div className="flex h-full flex-col">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 shadow-lg shadow-indigo-500/20">
                <AppLogo className="h-8 w-8" />
              </div>
              <div>
                <p className="text-[11px] font-bold uppercase tracking-[0.24em] text-indigo-300">
                  Smart Factory Platform
                </p>
                <p className="mt-1 text-xs font-semibold text-slate-500">Dashboard Monitoring Suite</p>
              </div>
            </div>

            <div className="mt-14 max-w-[440px]">
              <h1 className="text-[42px] font-black leading-[1.08] text-white">
                설비 흐름을
                <span className="block text-indigo-200">한눈에 읽는 대시보드</span>
              </h1>
              <p className="mt-6 text-[15px] leading-7 text-slate-400">
                장비 상태, 센서 이력, 위젯 데이터를 한 화면에 모아 현장의 변화를 빠르게 파악합니다.
              </p>
            </div>
          </div>
        </section>

        <section className="flex flex-col justify-center bg-[#0D1117]/82 p-8 sm:p-10 lg:p-12">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 shadow-lg shadow-indigo-500/20">
              <AppLogo className="h-7 w-7" />
            </div>
            <div>
              <div className="text-sm font-black uppercase tracking-tight text-white">대시보드</div>
              <div className="font-mono text-[10px] text-slate-500">보안 액세스</div>
            </div>
          </div>

          <div key={mode} className="auth-mode-panel">
            <div className="mb-8">
              <h2 className="text-3xl font-black tracking-tight text-white">
                {isSignup ? "계정 생성" : "로그인"}
              </h2>
              <p className="mt-2 text-sm text-slate-500">
                {isSignup ? "대시보드 계정을 등록하세요." : "모니터링 대시보드에 접근하세요."}
              </p>
            </div>

            <div className="mb-6 grid grid-cols-2 rounded-2xl border border-slate-800 bg-slate-950/60 p-1">
              {[
                { label: "로그인", value: "login" as const },
                { label: "계정 생성", value: "signup" as const },
              ].map((item) => (
                <button
                  key={item.value}
                  type="button"
                  onClick={() => switchMode(item.value)}
                  className={`h-10 rounded-xl text-sm font-bold transition-all duration-300 ${
                    mode === item.value
                      ? "bg-indigo-600 text-white shadow-lg shadow-indigo-500/20"
                      : "text-slate-500 hover:text-slate-200"
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>

            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="mb-2 block text-[10px] font-bold uppercase tracking-widest text-slate-500">
                  ID
                </label>
                <input
                  type="text"
                  value={id}
                  onChange={(event) => setId(event.target.value)}
                  placeholder={isSignup ? "영문 소문자, 숫자, 밑줄" : "ID"}
                  className="h-12 w-full rounded-2xl border border-slate-700/50 bg-slate-900/80 px-4 text-sm outline-none transition-all placeholder:text-slate-600 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                  autoComplete="username"
                />
              </div>

              {isSignup ? (
                <>
                  <div className="auth-field-enter">
                    <label className="mb-2 block text-[10px] font-bold uppercase tracking-widest text-slate-500">
                      이메일
                    </label>
                    <input
                      type="email"
                      value={email}
                      onChange={(event) => setEmail(event.target.value)}
                      placeholder="name@example.com"
                      className="h-12 w-full rounded-2xl border border-slate-700/50 bg-slate-900/80 px-4 text-sm outline-none transition-all placeholder:text-slate-600 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                      autoComplete="email"
                    />
                  </div>

                  <div className="auth-field-enter">
                    <label className="mb-2 block text-[10px] font-bold uppercase tracking-widest text-slate-500">
                      이름
                    </label>
                    <input
                      type="text"
                      value={fullName}
                      onChange={(event) => setFullName(event.target.value)}
                      placeholder="이름"
                      className="h-12 w-full rounded-2xl border border-slate-700/50 bg-slate-900/80 px-4 text-sm outline-none transition-all placeholder:text-slate-600 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                      autoComplete="name"
                    />
                  </div>
                </>
              ) : null}

              <div>
                <label className="mb-2 block text-[10px] font-bold uppercase tracking-widest text-slate-500">
                  비밀번호
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="비밀번호"
                    className="h-12 w-full rounded-2xl border border-slate-700/50 bg-slate-900/80 px-4 pr-16 text-sm outline-none transition-all placeholder:text-slate-600 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                    autoComplete={isSignup ? "new-password" : "current-password"}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    className="absolute inset-y-0 right-0 px-4 text-xs font-bold text-slate-500 transition-colors hover:text-white"
                  >
                    {showPassword ? "숨기기" : "보기"}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="flex h-12 w-full items-center justify-center gap-2 rounded-2xl bg-indigo-600 text-sm font-bold text-white shadow-lg shadow-indigo-500/20 transition-all hover:bg-indigo-500 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-400"
              >
                {isSubmitting ? "처리 중..." : isSignup ? "계정 생성" : "로그인"}
              </button>

              {import.meta.env.DEV && !isSignup && (
                <button
                  type="button"
                  onClick={onLocalTestLogin}
                  className="flex h-12 w-full items-center justify-center rounded-2xl border border-cyan-500/40 bg-cyan-500/10 text-sm font-bold text-cyan-100 transition-colors hover:bg-cyan-500/20"
                >
                  로컬 테스트로 대시보드 진입
                </button>
              )}
            </form>

            <div className="mt-8 flex flex-wrap items-center justify-between gap-4 border-t border-slate-800 pt-6">
              <div className="flex items-center gap-2 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1.5">
                <div className="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-500" />
                <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-500">
                  보안 채널
                </span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
