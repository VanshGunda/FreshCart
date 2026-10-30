import { useEffect, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import ProductCard from "../components/ProductCard";
import { wishlistApi } from "../services/storeService";
import { getApiError } from "../services/api";

export default function Wishlist(){
  const [products,setProducts]=useState([]); const [loading,setLoading]=useState(true); const [error,setError]=useState("");
  const { refreshCart }=useOutletContext(); const navigate=useNavigate();
  const load=()=>{setLoading(true);setError("");wishlistApi.list().then(r=>setProducts(r.data||[])).catch(e=>setError(getApiError(e))).finally(()=>setLoading(false));};
  useEffect(load,[]);
  if(loading)return <div className="container section"><div className="panel">Loading wishlist…</div></div>;
  return <div className="container section"><div className="section-head"><div><div className="eyebrow">SAVED FOR LATER</div><h1>My wishlist</h1><p className="muted">Keep favourite groceries ready for the next basket.</p></div><button className="btn btn-primary" onClick={()=>navigate("/shop")}>Continue shopping</button></div>{error&&<div className="error-box" role="alert">{error}<button className="btn btn-light" onClick={load}>Retry</button></div>}{products.length?<div className="product-grid">{products.map(p=><ProductCard key={p.id} product={p} onCartChange={refreshCart}/>)}</div>:<div className="panel empty"><h2>Your wishlist is empty</h2><p className="muted">Tap the heart on a product to save it here.</p><button className="btn btn-primary" onClick={()=>navigate("/shop")}>Explore groceries</button></div>}</div>
}
