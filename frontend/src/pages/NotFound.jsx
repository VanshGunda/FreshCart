import { Link } from "react-router-dom";
export default function NotFound(){return <div className="container section"><div className="panel empty"><h1>404 — Page not found</h1><p className="muted">This FreshCart page does not exist.</p><Link className="btn btn-primary" to="/">Back to home</Link></div></div>}
