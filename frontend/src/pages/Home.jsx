import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import ProductCard from "../components/ProductCard";
import { useOutletContext } from "react-router-dom";
import { productApi } from "../services/storeService";
import { getApiError } from "../services/api";

const categories = ["Fruits", "Vegetables", "Dairy", "Bakery", "Staples", "Beverages"];
const categoryIcons = { Fruits: "🍎", Vegetables: "🥦", Dairy: "🥛", Bakery: "🍞", Staples: "🍚", Beverages: "🧃" };

const heroImage = "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=1200&q=82";
const promoImages = {
  fresh: "https://images.unsplash.com/photo-1610832958506-aa56368176cf?auto=format&fit=crop&w=900&q=80",
  dairy: "https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=900&q=80",
};

export default function Home() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [recent, setRecent] = useState([]);
  const heroRef = useRef(null);
  const navigate = useNavigate();
  const { refreshCart } = useOutletContext();

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    productApi.catalog({ page: 0, size: 8, sort: "id,desc" })
      .then((response) => {
        if (active) setProducts(response.data?.content || []);
      })
      .catch((requestError) => {
        if (active) setError(getApiError(requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    try {
      const saved = (JSON.parse(localStorage.getItem("freshcart_recent") || "[]") || []).slice(0, 4);
      Promise.all(saved.map((item) => productApi.byId(item.id).then((response) => response.data).catch(() => null)))
        .then((items) => active && setRecent(items.filter(Boolean)));
    } catch {
      if (active) setRecent([]);
    }

    return () => { active = false; };
  }, []);

  const handleHeroMove = (event) => {
    const element = heroRef.current;
    if (!element || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const rect = element.getBoundingClientRect();
    const x = ((event.clientX - rect.left) / rect.width - 0.5) * 14;
    const y = ((event.clientY - rect.top) / rect.height - 0.5) * 10;
    element.style.setProperty("--mx", `${x}px`);
    element.style.setProperty("--my", `${y}px`);
  };

  const resetHeroMove = () => {
    const element = heroRef.current;
    if (!element) return;
    element.style.setProperty("--mx", "0px");
    element.style.setProperty("--my", "0px");
  };

  const submitSearch = (event) => {
    event.preventDefault();
    const query = String(new FormData(event.currentTarget).get("q") || "").trim();
    navigate(query ? `/shop?q=${encodeURIComponent(query)}` : "/shop");
  };

  return (
    <div className="home-page">
      <section className="hero-shell">
        <section className="hero" ref={heroRef} onPointerMove={handleHeroMove} onPointerLeave={resetHeroMove}>
          <div className="hero-grid">
            <div className="hero-copy">
              <div className="eyebrow">Farm fresh · delivered simply</div>
              <h1>Delivering fresh groceries to your doorstep.</h1>
              <p>Shop crisp produce, daily essentials and pantry favourites in one easy FreshCart basket.</p>
              <form className="hero-search" role="search" onSubmit={submitSearch}>
                <label className="sr-only" htmlFor="hero-q">Search groceries</label>
                <input id="hero-q" name="q" placeholder="Search milk, tomatoes, atta" />
                <button className="btn btn-primary btn-lg" type="submit">Search</button>
              </form>
              <div className="hero-actions">
                <button className="btn btn-accent btn-lg" onClick={() => navigate("/shop")}>Shop groceries</button>
              </div>
            </div>
            <div className="hero-art" aria-hidden="true">
              <div className="hero-art-card">
                <img src={heroImage} alt="" width="760" height="690" fetchPriority="high" />
              </div>
            </div>
          </div>
        </section>
      </section>

      <section className="service-strip">
        <div className="service-grid">
          <div className="service-item"><span className="service-icon">✓</span><span><strong>Secure checkout</strong><span>Razorpay verified payment</span></span></div>
          <div className="service-item"><span className="service-icon">✦</span><span><strong>Fresh selection</strong><span>Curated everyday groceries</span></span></div>
          <div className="service-item"><span className="service-icon">▦</span><span><strong>Simple shopping</strong><span>Fast search and easy cart</span></span></div>
          <div className="service-item"><span className="service-icon">→</span><span><strong>Track your order</strong><span>Clear status updates</span></span></div>
        </div>
      </section>

      <section className="category-section">
        <div className="container section">
          <div className="section-head">
            <div><div className="eyebrow">Shop by aisle</div><h2>Find your everyday essentials</h2></div>
            <button className="link-btn" onClick={() => navigate("/shop")}>View all →</button>
          </div>
          <div className="category-grid">
            {categories.map((category) => (
              <button key={category} className="category-tile" onClick={() => navigate(`/shop?category=${encodeURIComponent(category)}`)}>
                <span>{categoryIcons[category]}</span>
                <b>{category}</b>
              </button>
            ))}
          </div>
        </div>
      </section>

      <section className="container section">
        <div className="promo-grid">
          <article className="promo-card green">
            <img className="promo-card-image" src={promoImages.fresh} alt="Fresh fruit and vegetables" loading="lazy" width="900" height="780" />
            <div className="promo-copy">
              <div className="eyebrow">This week's fresh pick</div>
              <h3>Bring home more colour.</h3>
              <p>Bright produce and easy-to-shop staples for your weekly basket.</p>
              <button className="btn btn-primary" onClick={() => navigate("/shop?category=Fruits")}>Shop fresh picks</button>
            </div>
          </article>
          <article className="promo-card orange">
            <img className="promo-card-image" src={promoImages.dairy} alt="Fresh milk and dairy" loading="lazy" width="900" height="780" />
            <div className="promo-copy">
              <div className="eyebrow">Daily essentials</div>
              <h3>Keep the fridge ready.</h3>
              <p>Milk, dairy and bakery favourites for every morning.</p>
              <button className="btn btn-accent" onClick={() => navigate("/shop?category=Dairy")}>Browse dairy</button>
            </div>
          </article>
        </div>
      </section>

      <section className="container section">
        <div className="section-head">
          <div><div className="eyebrow">Made for the weekly shop</div><h2>Featured groceries</h2></div>
          <button className="link-btn" onClick={() => navigate("/shop")}>Shop all →</button>
        </div>
        {loading ? (
          <div className="skeleton-grid" role="status" aria-label="Loading products">
            {Array.from({ length: 8 }).map((_, index) => (
              <div className="skeleton-card" key={index}>
                <div className="skeleton-media" />
                <div className="skeleton-copy"><div className="skeleton-line medium" /><div className="skeleton-line short" /><div className="skeleton-line medium" /></div>
              </div>
            ))}
          </div>
        ) : error ? (
          <div className="panel error-box" role="alert"><span>{error}</span><button className="btn btn-light" onClick={() => window.location.reload()}>Retry</button></div>
        ) : (
          <div className="product-grid">{products.map((product) => <ProductCard key={product.id} product={product} onCartChange={refreshCart} />)}</div>
        )}
      </section>

      {recent.length > 0 && (
        <section className="container section">
          <div className="section-head">
            <div><div className="eyebrow">Pick up where you left off</div><h2>Recently viewed</h2></div>
            <button className="link-btn" onClick={() => navigate("/shop")}>Browse more →</button>
          </div>
          <div className="product-grid">{recent.map((product) => <ProductCard key={product.id} product={product} onCartChange={refreshCart} />)}</div>
        </section>
      )}

      <section className="container section home-extra">
        <div className="home-extra-card">
          <div><div className="eyebrow">Ready when you are</div><h3>Your next grocery run starts here.</h3><p>Browse the complete catalog and build your basket in a few quick taps.</p></div>
          <button className="btn btn-accent btn-lg" onClick={() => navigate("/shop")}>Explore FreshCart</button>
        </div>
      </section>
    </div>
  );
}
