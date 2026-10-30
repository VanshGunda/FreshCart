import { useEffect, useMemo, useState } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import { productApi } from "../services/storeService";
import { getApiError } from "../services/api";
import ProductCard from "../components/ProductCard";
import { useOutletContext } from "react-router-dom";

const categories = ["Fruits", "Vegetables", "Dairy", "Bakery", "Staples", "Beverages"];
const initialFilters = { q: "", category: "", brand: "", min: "", max: "", sort: "" };

function filtersFromParams(searchParams) {
  return {
    q: searchParams.get("q") || "",
    category: searchParams.get("category") || "",
    brand: searchParams.get("brand") || "",
    min: searchParams.get("min") || "",
    max: searchParams.get("max") || "",
    sort: searchParams.get("sort") || "",
  };
}

export default function Shop() {
  const { refreshCart } = useOutletContext();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const [products, setProducts] = useState([]);
  const [pageInfo, setPageInfo] = useState({ page: 0, size: 24, totalElements: 0, totalPages: 0, last: true });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filters, setFilters] = useState(initialFilters);
  const [suggestions, setSuggestions] = useState([]);

  const paramString = params.toString();
  const urlFilters = useMemo(() => filtersFromParams(new URLSearchParams(paramString)), [paramString]);
  const page = Math.max(0, Number(params.get("page") || 0));

  useEffect(() => {
    setFilters(urlFilters);
  }, [urlFilters]);

  useEffect(() => {
    const q=filters.q.trim();
    if(q.length<2){setSuggestions([]);return;}
    let live=true;
    const timer=setTimeout(()=>productApi.suggestions(q).then(r=>live&&setSuggestions(r.data||[])).catch(()=>live&&setSuggestions([])),250);
    return ()=>{live=false;clearTimeout(timer);};
  }, [filters.q]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    const requestParams = {
      page,
      size: 24,
      search: urlFilters.q.trim(),
      category: urlFilters.category,
      brand: urlFilters.brand.trim(),
      min: urlFilters.min,
      max: urlFilters.max,
      sort: urlFilters.sort || "id,desc",
    };

    if ((requestParams.min === "") !== (requestParams.max === "")) {
      setProducts([]);
      setError("Enter both minimum and maximum price.");
      setLoading(false);
      return () => { active = false; };
    }

    productApi.catalog(requestParams)
      .then((response) => {
        if (!active) return;
        const data = response.data || {};
        setProducts(data.content || []);
        setPageInfo({
          page: data.page ?? page,
          size: data.size ?? 24,
          totalElements: data.totalElements ?? 0,
          totalPages: data.totalPages ?? 0,
          last: Boolean(data.last),
        });
      })
      .catch((requestError) => {
        if (active) {
          setProducts([]);
          setError(getApiError(requestError));
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => { active = false; };
  }, [page, urlFilters]);

  const title = useMemo(() => (
    urlFilters.category ? `${urlFilters.category} groceries` :
      urlFilters.q ? `Results for “${urlFilters.q}”` :
        urlFilters.brand ? `${urlFilters.brand} groceries` : "All groceries"
  ), [urlFilters]);

  const submit = (event) => {
    event.preventDefault();
    if ((filters.min === "") !== (filters.max === "")) {
      setError("Enter both minimum and maximum price.");
      return;
    }

    const next = {};
    if (filters.q.trim()) next.q = filters.q.trim();
    if (filters.category) next.category = filters.category;
    if (filters.brand.trim()) next.brand = filters.brand.trim();
    if (filters.min !== "" && filters.max !== "") {
      next.min = filters.min;
      next.max = filters.max;
    }
    if (filters.sort) next.sort = filters.sort;
    next.page = "0";
    setParams(next);
  };

  const clear = () => {
    setFilters(initialFilters);
    setParams({});
  };

  const goToPage = (nextPage) => {
    const next = Object.fromEntries(params.entries());
    if (nextPage > 0) next.page = String(nextPage);
    else delete next.page;
    setParams(next);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const firstItem = pageInfo.totalElements === 0 ? 0 : pageInfo.page * pageInfo.size + 1;
  const lastItem = Math.min((pageInfo.page + 1) * pageInfo.size, pageInfo.totalElements);

  const paymentNotice = location.state?.paymentStatus ? {
    success: location.state.paymentStatus === "success",
    message: location.state.message,
  } : null;

  return <div className="container section shop-page">
    <div className="section-head">
      <div><h1>{title}</h1></div>
      <span className="result-count" aria-live="polite">{loading ? "Loading…" : `${firstItem}-${lastItem} of ${pageInfo.totalElements}`}</span>
    </div>

    {paymentNotice && <div className={paymentNotice.success ? "success-box" : "error-box"} role={paymentNotice.success ? "status" : "alert"}>{paymentNotice.message}</div>}

    <div className="filters panel">
      <form onSubmit={submit} className="filter-row" role="search" aria-label="Filter groceries">
        <label className="sr-only" htmlFor="shop-search">Search groceries</label>
        <div className="search-suggest"><input id="shop-search" list="freshcart-search-suggestions" value={filters.q} onChange={(event) => setFilters((current) => ({ ...current, q: event.target.value, category: "" }))} placeholder="Search groceries" autoComplete="off" /><datalist id="freshcart-search-suggestions">{suggestions.map(p=><option key={p.id} value={p.name}/>)}</datalist></div>
        <label className="sr-only" htmlFor="shop-category">Category</label>
        <select id="shop-category" value={filters.category} onChange={(event) => setFilters((current) => ({ ...current, category: event.target.value, q: "" }))}><option value="">Category</option>{categories.map((category) => <option key={category} value={category}>{category}</option>)}</select>
        <label className="sr-only" htmlFor="shop-brand">Brand</label>
        <input id="shop-brand" value={filters.brand} onChange={(event) => setFilters((current) => ({ ...current, brand: event.target.value }))} placeholder="Brand" />
        <label className="sr-only" htmlFor="shop-min">Minimum price</label>
        <input id="shop-min" type="number" min="0" step="0.01" value={filters.min} onChange={(event) => setFilters((current) => ({ ...current, min: event.target.value }))} placeholder="Min ₹" />
        <label className="sr-only" htmlFor="shop-max">Maximum price</label>
        <input id="shop-max" type="number" min="0" step="0.01" value={filters.max} onChange={(event) => setFilters((current) => ({ ...current, max: event.target.value }))} placeholder="Max ₹" />
        <label className="sr-only" htmlFor="shop-sort">Sort groceries</label>
        <select id="shop-sort" value={filters.sort} onChange={(event) => setFilters((current) => ({ ...current, sort: event.target.value }))}><option value="">Sort</option><option value="name,asc">Name A–Z</option><option value="price,asc">Price low to high</option><option value="price,desc">Price high to low</option></select>
        <button className="btn btn-primary" type="submit">Apply</button>
        <button className="btn btn-light" type="button" onClick={clear}>Reset</button>
      </form>
    </div>

    {loading ? <div className="product-grid product-grid-skeleton" aria-label="Loading groceries">{Array.from({ length: 12 }, (_, index) => <div className="product-card skeleton-card" key={index}><div className="skeleton-image" /><div className="product-body"><div className="skeleton-line" /><div className="skeleton-line short" /><div className="skeleton-line price" /></div></div>)}</div>
      : error ? <div className="panel error-box" role="alert"><p>{error}</p><button className="btn btn-primary" onClick={() => window.location.reload()}>Retry</button></div>
      : products.length ? <>
        <div className="product-grid">{products.map((product) => <ProductCard key={product.id} product={product} onCartChange={refreshCart} />)}</div>
        <div className="pagination" aria-label="Product pages">
          <button className="btn btn-light" disabled={pageInfo.page <= 0} onClick={() => goToPage(pageInfo.page - 1)}>← Previous</button>
          <span>Page {pageInfo.page + 1} of {Math.max(1, pageInfo.totalPages)}</span>
          <button className="btn btn-light" disabled={pageInfo.last} onClick={() => goToPage(pageInfo.page + 1)}>Next →</button>
        </div>
      </>
      : <div className="panel empty"><h2>No groceries matched</h2><p className="muted">Try a broader search or reset the filters.</p><button className="btn btn-primary" onClick={clear}>Clear filters</button></div>}
  </div>;
}
