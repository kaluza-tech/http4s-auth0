// TODO: decice which ones we want to remove
wartremoverErrors in (Compile, compile) ++= Warts.allBut(Wart.Var, Wart.ImplicitParameter, Wart.DefaultArguments)
// TODO: set this to true once we get close to being clean
scalastyleFailOnWarning := false
