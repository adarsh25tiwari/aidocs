import { useState } from "react";
import { login, signup } from "../api.js";

export default function AuthPage({ onLoggedIn }) {
    const [mode, setMode] = useState("login"); // login | signup
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [info, setInfo] = useState("");
    const [loading, setLoading] = useState(false);

    const isSignup = mode === "signup";

    async function submit(e) {
        e.preventDefault();
        setError("");
        setInfo("");
        setLoading(true);
        try {
            if (isSignup) {
                await signup(username.trim(), email.trim(), password);
                // signup does not return a token, so log in right away
            }
            const user = await login(username.trim(), password);
            onLoggedIn(user);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    function switchMode() {
        setMode(isSignup ? "login" : "signup");
        setError("");
        setInfo("");
    }

    return (
        <div className="auth-wrap">
            <form className="auth-card" onSubmit={submit}>
                <div className="brand">
                    <span className="logo">🧠</span>
                    <div>
                        <h1>AiDocs</h1>
                        <p>Chat with your documents</p>
                    </div>
                </div>

                <h2>{isSignup ? "Create your account" : "Welcome back"}</h2>

                <label>
                    Username
                    <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required autoComplete="username" />
                </label>

                {isSignup && (
                    <label>
                        Email
                        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
                    </label>
                )}

                <label>
                    Password
                    <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        autoComplete={isSignup ? "new-password" : "current-password"}
                    />
                </label>

                {error && <div className="notice error">⚠ {error}</div>}
                {info && <div className="notice ok">{info}</div>}

                <button className="send auth-submit" type="submit" disabled={loading}>
                    {loading ? "Please wait…" : isSignup ? "Sign up" : "Log in"}
                </button>

                <p className="auth-switch">
                    {isSignup ? "Already have an account?" : "New here?"}{" "}
                    <button type="button" className="link" onClick={switchMode}>
                        {isSignup ? "Log in" : "Create an account"}
                    </button>
                </p>
            </form>
        </div>
    );
}
