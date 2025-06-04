@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.0.6')
import com.fasterxml.jackson.databind.*

class User {
String name
}
user = new User()
user.name = "Zemian"

mapper = new ObjectMapper()

// TODO: why when I enable this line, the println below will not work?
mapper.writeValue(System.out, user)
println("aggiunta diversa dal main")

user2 = mapper.readValue('{"name":"Zemian"}', User.class)
println("user2 " + user2)
println("seconda aggiunta diversa dal main ciao")
