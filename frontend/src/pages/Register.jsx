import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authApi } from "../services/storeService";
import { getApiError } from "../services/api";

const passwordRule=/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=!]).{8,72}$/;
export default function Register(){
 const [username,setUsername]=useState("");const [password,setPassword]=useState("");const [error,setError]=useState("");const [loading,setLoading]=useState(false);const navigate=useNavigate();
 const submit=async e=>{e.preventDefault();setError("");if(!username.trim()){setError("Email is required.");return;}if(!passwordRule.test(password)){setError("Password must contain at least 8 characters, one uppercase, one lowercase, one digit and one special character (@#$%^&+=!).");return;}setLoading(true);try{await authApi.register({username:username.trim(),password});navigate("/verify-otp",{state:{username:username.trim()}});}catch(e){setError(getApiError(e));}finally{setLoading(false);}};
 return <div className="auth-page"><div className="auth-card"><div className="brand auth-brand"><span className="brand-mark">F</span><span>Fresh<span>Cart</span></span></div><h1>Create your account</h1><p className="muted">We’ll send a one-time code to verify your email.</p><form className="auth-form" onSubmit={submit}><label>Email<input type="email" value={username} onChange={e=>setUsername(e.target.value)} placeholder="you@example.com" autoComplete="email" required/></label><label>Password<input type="password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="FreshCart@123" autoComplete="new-password" required/></label><div className="hint">8–72 chars · uppercase · lowercase · number · special character</div>{error&&<div className="error-box">{error}</div>}<button className="btn btn-primary btn-lg" disabled={loading}>{loading?"Creating…":"Create account"}</button></form><p className="auth-footer">Already registered? <Link to="/login">Sign in</Link></p></div></div>;
}
