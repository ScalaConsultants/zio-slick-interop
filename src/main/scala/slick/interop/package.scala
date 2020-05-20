package slick

import zio.Has

package object interop {
  type DatabaseProvider = Has[DatabaseProvider.Service]
}
