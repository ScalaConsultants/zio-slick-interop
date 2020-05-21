package slick.interop.zio

import zio.Has

package object tests {
  type ItemRepository = Has[ItemRepository.Service]
}
