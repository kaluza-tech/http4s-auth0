package com.ovoenergy.evse

trait BaseError extends Product with Serializable {
  def msg: String
}
