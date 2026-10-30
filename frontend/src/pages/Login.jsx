import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { authApi } from "../services/storeService";
import { getApiError } from "../services/api";
import { useAuth } from "../context/AuthContext";

export default function Login(){
  const [username,setUsername]=useState("");const [password,setPassword]=useState("");const [error,setError]=useState("");const [loading,setLoading]=useState(false);
  const {login}=useAuth();const navigate=useNavigate();const location=useLocation();
  const submit=async e=>{e.preventDefault();setError("");if(!username.trim()||!password){setError("Email and password are required.");return;}setLoading(true);try{const r=await authApi.login({username:username.trim(),password});login(r.data);navigate(location.state?.from||"/shop",{replace:true});}catch(e){setError(getApiError(e));}finally{setLoading(false);}};
  return <AuthShell title="Welcome back" subtitle="Sign in to your FreshCart account."><form className="auth-form" onSubmit={submit}><label>Email<input type="email" value={username} onChange={e=>setUsername(e.target.value)} placeholder="you@example.com" autoComplete="email" required/></label><label>Password<input type="password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="Your password" autoComplete="current-password" required/></label>{error&&<div className="error-box">{error}</div>}<button className="btn btn-primary btn-lg" disabled={loading}>{loading?"Signing in…":"Sign in"}</button></form><p className="auth-footer">New to FreshCart? <Link to="/register">Create an account</Link></p></AuthShell>;
}

function AuthShell({title,subtitle,children}){return <div className="auth-page"><div className="auth-card"><div className="brand auth-brand"><span className="brand-mark">F</span><span>Fresh<span>Cart</span></span></div><h1>{title}</h1><p className="muted">{subtitle}</p>{children}</div></div>}
