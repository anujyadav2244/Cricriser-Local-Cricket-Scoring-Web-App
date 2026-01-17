import { BrowserRouter } from "react-router-dom";
import AppRoutes from "@/routes/AppRoutes";
import { Toaster } from "./components/ui/sonner";

export default function App() {
  return (
    <BrowserRouter>
      <Toaster richColors position="bottom-right" />
      <AppRoutes />
    </BrowserRouter>
  );
}
