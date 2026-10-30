import { Link } from "react-router-dom";
export default function Forbidden(){return <div className="container section"><div className="panel empty"><h1>403 — Access denied</h1><p className="muted">Your FreshCart account is not authorized to use this page.</p><Link className="btn btn-primary" to="/">Back to FreshCart</Link></div></div>}
